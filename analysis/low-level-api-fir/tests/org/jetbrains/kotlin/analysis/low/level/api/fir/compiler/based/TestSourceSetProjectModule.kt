/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.compiler.based

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import org.jetbrains.kotlin.analysis.project.structure.LibraryProjectModule
import org.jetbrains.kotlin.analysis.project.structure.LibrarySourceProjectModule
import org.jetbrains.kotlin.analysis.project.structure.ProjectModule
import org.jetbrains.kotlin.analysis.project.structure.SourceSetProjectModule
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.jvmModularRoots
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.PlatformDependentAnalyzerServices
import org.jetbrains.kotlin.test.frontend.fir.getAnalyzerServices
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.jetbrains.kotlin.utils.addIfNotNull
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

@OptIn(ExperimentalStdlibApi::class)
internal class TestSourceSetProjectModule(
    override val project: Project,
    val testModule: TestModule,
    val testFilesToKtFiles: Map<TestFile, KtFile>,
    testServices: TestServices,
) : SourceSetProjectModule {
    private val moduleProvider = testServices.projectModuleProvider
    private val compilerConfigurationProvider = testServices.compilerConfigurationProvider
    private val configuration = compilerConfigurationProvider.getCompilerConfiguration(testModule)

    val ktFiles = testFilesToKtFiles.values.toSet()

    override val moduleName: String
        get() = testModule.name

    override val regularDependencies: List<ProjectModule> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        buildList {
            testModule.allDependencies.mapTo(this) { moduleProvider.getModule(it.moduleName) }
            addIfNotNull(
                libraryByRoots(
                    (configuration.jvmModularRoots + configuration.jvmClasspathRoots).map(File::toPath)
                )
            )
        }
    }
    override val refinementDependencies: List<ProjectModule> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        testModule.dependsOnDependencies
            .map { moduleProvider.getModule(it.moduleName) }
    }
    override val friendDependencies: List<ProjectModule> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        buildList {
            testModule.friendDependencies.mapTo(this) { moduleProvider.getModule(it.moduleName) }
            addIfNotNull(
                libraryByRoots(configuration[JVMConfigurationKeys.FRIEND_PATHS].orEmpty().map(Paths::get))
            )
        }
    }

    private fun libraryByRoots(roots: List<Path>): LibraryByRoots? {
        if (roots.isEmpty()) return null
        return LibraryByRoots(
            roots,
            this@TestSourceSetProjectModule,
            project,
        )
    }

    override val contentScope: GlobalSearchScope =
        TopDownAnalyzerFacadeForJVM.newModuleSearchScope(project, testFilesToKtFiles.values)

    override val languageVersionSettings: LanguageVersionSettings
        get() = testModule.languageVersionSettings

    override val platform: TargetPlatform
        get() = testModule.targetPlatform

    override val analyzerServices: PlatformDependentAnalyzerServices
        get() = testModule.targetPlatform.getAnalyzerServices()
}

private class LibraryByRoots(
    private val roots: List<Path>,
    private val sourceModule: SourceSetProjectModule,
    override val project: Project,
) : LibraryProjectModule {
    override val libraryName: String get() = "Test Library"
    override val regularDependencies: List<ProjectModule> get() = emptyList()
    override val refinementDependencies: List<ProjectModule> get() = emptyList()
    override val friendDependencies: List<ProjectModule> get() = emptyList()
    override val contentScope: GlobalSearchScope get() = ProjectScope.getLibrariesScope(project)
    override val platform: TargetPlatform get() = sourceModule.platform
    override val analyzerServices: PlatformDependentAnalyzerServices get() = sourceModule.analyzerServices
    override fun getBinaryRoots(): Collection<Path> = roots
    override val librarySources: LibrarySourceProjectModule? get() = null
}

