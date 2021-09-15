/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.low.level.api.fir.FirPhaseRunner
import org.jetbrains.kotlin.analysis.low.level.api.fir.IdeFirPhaseManager
import org.jetbrains.kotlin.analysis.low.level.api.fir.IdeSessionComponents
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.services.createPackagePartProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.services.createSealedInheritorsProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.FirFileBuilder
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.ModuleFileCacheImpl
import org.jetbrains.kotlin.analysis.low.level.api.fir.fir.caches.FirThreadSafeCachesFactory
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.FirLazyDeclarationResolver
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkCanceled
import org.jetbrains.kotlin.analysis.project.structure.*
import org.jetbrains.kotlin.analysis.providers.createDeclarationProvider
import org.jetbrains.kotlin.analysis.providers.createPackageProvider
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmTypeMapper
import org.jetbrains.kotlin.fir.caches.FirCachesFactory
import org.jetbrains.kotlin.fir.checkers.registerExtendedCommonCheckers
import org.jetbrains.kotlin.fir.declarations.SealedClassInheritorsProvider
import org.jetbrains.kotlin.fir.deserialization.ModuleDataProvider
import org.jetbrains.kotlin.fir.java.JavaSymbolProvider
import org.jetbrains.kotlin.fir.java.deserialization.KotlinDeserializedJvmSymbolsProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirDependenciesSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirCloneableSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirCompositeSymbolProvider
import org.jetbrains.kotlin.fir.resolve.scopes.wrapScopeWithJvmMapped
import org.jetbrains.kotlin.fir.resolve.symbolProvider
import org.jetbrains.kotlin.fir.resolve.transformers.FirPhaseCheckingPhaseManager
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.session.*
import org.jetbrains.kotlin.fir.symbols.FirPhaseManager
import org.jetbrains.kotlin.load.java.createJavaClassFinder
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinderFactory
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleResolver
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatformAnalyzerServices
import java.nio.file.Path

@OptIn(PrivateSessionConstructor::class, SessionConfiguration::class)
internal object FirIdeSessionFactory {
    fun createSourcesSession(
        project: Project,
        module: SourceSetProjectModule,
        builtinsAndCloneableSession: FirIdeBuiltinsAndCloneableSession,
        firPhaseRunner: FirPhaseRunner,
        sessionInvalidator: FirSessionInvalidator,
        builtinTypes: BuiltinTypes,
        sessionsCache: MutableMap<SourceSetProjectModule, FirIdeSourcesSession>,
        isRootModule: Boolean,
        librariesCache: LibrariesCache,
        configureSession: (FirIdeSession.() -> Unit)? = null
    ): FirIdeSourcesSession {
        sessionsCache[module]?.let { return it }
        val languageVersionSettings = module.languageVersionSettings
        val scopeProvider = FirKotlinScopeProvider(::wrapScopeWithJvmMapped)
        val firBuilder = FirFileBuilder(scopeProvider, firPhaseRunner)
        val contentScope = module.contentScope
        val dependentModules = module.regularDependenceisOfType<SourceSetProjectModule>()
        val session = FirIdeSourcesSession(module, project, firBuilder, builtinTypes)
        sessionsCache[module] = session

        return session.apply session@{
            val moduleData = ModuleProjectBasedModuleData(module).apply { bindSession(this@session) }
            registerModuleData(moduleData)

            val cache = ModuleFileCacheImpl(this)
            val firPhaseManager = IdeFirPhaseManager(FirLazyDeclarationResolver(firFileBuilder), cache, sessionInvalidator)

            registerIdeComponents(project)
            registerCommonComponents(languageVersionSettings)
            registerCommonJavaComponents(JavaModuleResolver.getInstance(project))
            registerResolveComponents()

            val provider = FirIdeProvider(
                project,
                this,
                module,
                scopeProvider,
                firFileBuilder,
                cache,
                project.createDeclarationProvider(contentScope),
                project.createPackageProvider(contentScope),
            )

            register(FirProvider::class, provider)
            register(FirIdeProvider::class, provider)

            register(FirPhaseManager::class, firPhaseManager)

            @OptIn(ExperimentalStdlibApi::class)
            val dependentProviders = buildList {
                add(
                    createModuleLibrariesSession(
                        module,
                        project,
                        builtinsAndCloneableSession,
                        builtinTypes,
                        librariesCache,
                        languageVersionSettings = languageVersionSettings,
                        configureSession = configureSession,
                    ).symbolProvider
                )
                dependentModules
                    .mapTo(this) {
                        createSourcesSession(
                            project,
                            it,
                            builtinsAndCloneableSession,
                            firPhaseRunner,
                            sessionInvalidator,
                            builtinTypes,
                            sessionsCache,
                            isRootModule = false,
                            librariesCache = librariesCache,
                            configureSession = configureSession,
                        ).symbolProvider
                    }
            }

            val dependencyProvider = DependentModuleProviders(this, dependentProviders)

            register(
                FirSymbolProvider::class,
                FirModuleWithDependenciesSymbolProvider(
                    this,
                    providers = listOf(
                        provider.symbolProvider,
                        JavaSymbolProvider(this, moduleData, project.createJavaClassFinder(contentScope)),
                    ),
                    dependencyProvider
                )
            )

            register(FirDependenciesSymbolProvider::class, dependencyProvider)
            register(FirJvmTypeMapper::class, FirJvmTypeMapper(this))

            registerJavaSpecificResolveComponents()
            FirSessionFactory.FirSessionConfigurator(this).apply {
                if (isRootModule) {
                    registerExtendedCommonCheckers()
                }
            }.configure()
            configureSession?.invoke(this)
        }
    }

    private fun createModuleLibrariesSession(
        sourceModule: SourceSetProjectModule,
        project: Project,
        builtinsAndCloneableSession: FirIdeBuiltinsAndCloneableSession,
        builtinTypes: BuiltinTypes,
        librariesCache: LibrariesCache,
        languageVersionSettings: LanguageVersionSettings = LanguageVersionSettingsImpl.DEFAULT,
        configureSession: (FirIdeSession.() -> Unit)?,
    ): FirIdeLibrariesSession = librariesCache.cached(sourceModule) {
        checkCanceled()
        val searchScope = project.moduleScopeProvider.getModuleLibrariesScope(sourceModule)
        FirIdeLibrariesSession(project, builtinTypes).apply session@{
            registerModuleData(ModuleProjectBasedModuleData(sourceModule).apply { bindSession(this@session) })
            registerIdeComponents(project)
            register(FirPhaseManager::class, FirPhaseCheckingPhaseManager)
            registerCommonComponents(languageVersionSettings)
            registerCommonJavaComponents(JavaModuleResolver.getInstance(project))
            registerJavaSpecificResolveComponents()

            val kotlinSymbolProvider = KotlinDeserializedJvmSymbolsProvider(
                this@session,
                moduleDataProvider = createModuleDataProvider(sourceModule, this),
                kotlinScopeProvider = FirKotlinScopeProvider(::wrapScopeWithJvmMapped),
                packagePartProvider = project.createPackagePartProvider(searchScope),
                kotlinClassFinder = VirtualFileFinderFactory.getInstance(project).create(searchScope),
                javaClassFinder = project.createJavaClassFinder(searchScope)
            )
            val symbolProvider = FirCompositeSymbolProvider(this, listOf(kotlinSymbolProvider, builtinsAndCloneableSession.symbolProvider))
            register(FirProvider::class, FirIdeLibrariesSessionProvider(symbolProvider))
            register(FirSymbolProvider::class, symbolProvider)
            register(FirJvmTypeMapper::class, FirJvmTypeMapper(this))
            configureSession?.invoke(this)
        }
    }

    private fun createModuleDataProvider(sourceModule: SourceSetProjectModule, session: FirIdeSession): ModuleDataProvider {
        val dependencyList = DependencyListForCliModule.build(
            Name.special("<${sourceModule.moduleDescription}>"),
            sourceModule.platform,
            sourceModule.analyzerServices
        ) {
            dependencies(sourceModule.regularDependencies.extractLibraryPaths())
            friendDependencies(sourceModule.friendDependencies.extractLibraryPaths())
            dependsOnDependencies(sourceModule.refinementDependencies.extractLibraryPaths())
        }
        val dataProvider = dependencyList.moduleDataProvider
        dataProvider.allModuleData.forEach { it.bindSession(session) }
        return dataProvider
    }

    fun createBuiltinsAndCloneableSession(
        project: Project,
        builtinTypes: BuiltinTypes,
        languageVersionSettings: LanguageVersionSettings = LanguageVersionSettingsImpl.DEFAULT,
        configureSession: (FirIdeSession.() -> Unit)? = null,
    ): FirIdeBuiltinsAndCloneableSession {
        return FirIdeBuiltinsAndCloneableSession(project, builtinTypes).apply session@{
            val moduleData = FirModuleDataImpl(
                Name.special("<builtins module>"),
                emptyList(),
                emptyList(),
                emptyList(),
                JvmPlatforms.unspecifiedJvmPlatform,
                JvmPlatformAnalyzerServices
            ).apply {
                bindSession(this@session)
            }
            registerIdeComponents(project)
            register(FirPhaseManager::class, FirPhaseCheckingPhaseManager)
            registerCommonComponents(languageVersionSettings)
            registerModuleData(moduleData)

            val kotlinScopeProvider = FirKotlinScopeProvider(::wrapScopeWithJvmMapped)
            val symbolProvider = FirCompositeSymbolProvider(
                this,
                listOf(
                    FirIdeBuiltinSymbolProvider(this, moduleData, kotlinScopeProvider),
                    FirCloneableSymbolProvider(this, moduleData, kotlinScopeProvider),
                )
            )
            register(FirSymbolProvider::class, symbolProvider)
            register(FirProvider::class, FirIdeBuiltinsAndCloneableSessionProvider(symbolProvider))
            register(FirJvmTypeMapper::class, FirJvmTypeMapper(this))
            configureSession?.invoke(this)
        }
    }

    private fun FirIdeSession.registerIdeComponents(project: Project) {
        register(IdeSessionComponents::class, IdeSessionComponents.create(this))
        register(FirCachesFactory::class, FirThreadSafeCachesFactory)
        register(SealedClassInheritorsProvider::class, project.createSealedInheritorsProvider())
    }
}

private fun List<ProjectModule>.extractLibraryPaths(): List<Path> =
    asSequence()
        .filterIsInstance<BinaryProjectModule>()
        .flatMap { it.getBinaryRoots() }
        .toList()

@Deprecated(
    "This is a dirty hack used only for one usage (building fir for psi from stubs) and it should be removed after fix of that usage",
    level = DeprecationLevel.ERROR
)
@OptIn(PrivateSessionConstructor::class)
fun createEmptySession(): FirSession {
    return object : FirSession(null, Kind.Source) {}.apply {
        val moduleData = FirModuleDataImpl(
            Name.identifier("<stub module>"),
            dependencies = emptyList(),
            dependsOnDependencies = emptyList(),
            friendDependencies = emptyList(),
            platform = JvmPlatforms.unspecifiedJvmPlatform,
            analyzerServices = JvmPlatformAnalyzerServices
        )
        registerModuleData(moduleData)
        moduleData.bindSession(this)
    }
}
