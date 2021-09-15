/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootModificationTracker
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirModuleResolveState
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.FirLazyDeclarationResolver
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.FirIdeSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.FirIdeSessionProviderStorage
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.cachedValue
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.getValue
import org.jetbrains.kotlin.analysis.project.structure.ProjectModule
import org.jetbrains.kotlin.analysis.project.structure.SourceSetProjectModule
import org.jetbrains.kotlin.analysis.providers.createProjectWideOutOfBlockModificationTracker
import java.util.concurrent.ConcurrentHashMap

internal class FirIdeResolveStateService(project: Project) {
    private val sessionProviderStorage = FirIdeSessionProviderStorage(project)

    private val stateCache by cachedValue(
        project,
        project.createProjectWideOutOfBlockModificationTracker(),
        ProjectRootModificationTracker.getInstance(project),
    ) {
        ConcurrentHashMap<ProjectModule, FirModuleResolveStateImpl>()
    }

    fun getResolveState(module: ProjectModule): FirModuleResolveStateImpl =
        stateCache.computeIfAbsent(module) { createResolveStateFor(module, sessionProviderStorage) }

    companion object {
        fun getInstance(project: Project): FirIdeResolveStateService =
            ServiceManager.getService(project, FirIdeResolveStateService::class.java)

        internal fun createResolveStateFor(
            module: ProjectModule,
            sessionProviderStorage: FirIdeSessionProviderStorage,
            configureSession: (FirIdeSession.() -> Unit)? = null,
        ): FirModuleResolveStateImpl {
            if (module !is SourceSetProjectModule) {
                error("Creating FirModuleResolveState is not yet supported for $module")
            }
            val sessionProvider = sessionProviderStorage.getSessionProvider(module, configureSession)
            val firFileBuilder = sessionProvider.rootModuleSession.firFileBuilder
            return FirModuleResolveStateImpl(
                sessionProviderStorage.project,
                module,
                sessionProvider,
                firFileBuilder,
                FirLazyDeclarationResolver(firFileBuilder),
            )
        }
    }
}

@TestOnly
fun createResolveStateForNoCaching(
    module: ProjectModule,
    project: Project,
    configureSession: (FirIdeSession.() -> Unit)? = null,
): FirModuleResolveState =
    FirIdeResolveStateService.createResolveStateFor(
        module = module,
        sessionProviderStorage = FirIdeSessionProviderStorage(project),
        configureSession = configureSession
    )


