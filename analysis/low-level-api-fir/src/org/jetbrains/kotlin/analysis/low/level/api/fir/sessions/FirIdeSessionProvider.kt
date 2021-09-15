/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.low.level.api.fir.annotations.Immutable
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.ModuleFileCache
import org.jetbrains.kotlin.analysis.project.structure.ProjectModule
import org.jetbrains.kotlin.analysis.project.structure.SourceSetProjectModule
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionProvider

@Immutable
class FirIdeSessionProvider internal constructor(
    val project: Project,
    internal val rootModuleSession: FirIdeSourcesSession,
    val sessions: Map<SourceSetProjectModule, FirIdeSession>
) : FirSessionProvider() {
    override fun getSession(moduleData: FirModuleData): FirSession? =
        sessions[moduleData.module]

    fun getSession(module: ProjectModule): FirSession? =
        sessions[module]

    internal fun getModuleCache(module: SourceSetProjectModule): ModuleFileCache =
        (sessions.getValue(module) as FirIdeSourcesSession).cache
}
