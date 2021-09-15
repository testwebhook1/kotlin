/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions

import org.jetbrains.kotlin.analysis.project.structure.ProjectModule
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.resolve.PlatformDependentAnalyzerServices

internal class ModuleProjectBasedModuleData(
    val module: ProjectModule,
) : FirModuleData() {
    override val name: Name get() = Name.special("<${module.moduleDescription}>")

    override val dependencies: List<FirModuleData> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        module.regularDependencies.map(::ModuleProjectBasedModuleData)
    }

    override val dependsOnDependencies: List<FirModuleData> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        module.refinementDependencies.map(::ModuleProjectBasedModuleData)
    }

    override val friendDependencies: List<FirModuleData> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        module.refinementDependencies.map(::ModuleProjectBasedModuleData)
    }

    override val platform: TargetPlatform get() = module.platform

    override val analyzerServices: PlatformDependentAnalyzerServices get() = module.analyzerServices
}
