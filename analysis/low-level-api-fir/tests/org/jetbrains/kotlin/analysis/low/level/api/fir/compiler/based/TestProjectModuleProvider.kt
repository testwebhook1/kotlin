/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.compiler.based

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestService
import org.jetbrains.kotlin.test.services.TestServices

class TestProjectModuleProvider(
    private val testServices: TestServices
) : TestService {
    private val cache = mutableMapOf<String, TestSourceSetProjectModule>()

    fun registerModuleInfo(project: Project, testModule: TestModule, ktFiles: Map<TestFile, KtFile>) {
        cache[testModule.name] = TestSourceSetProjectModule(project, testModule, ktFiles, testServices)
    }

    internal fun getModuleInfoByKtFile(ktFile: KtFile): TestSourceSetProjectModule =
        cache.values.first { moduleSourceInfo ->
            ktFile in moduleSourceInfo.ktFiles
        }

    internal fun getModule(moduleName: String): TestSourceSetProjectModule =
        cache.getValue(moduleName)
}

val TestServices.projectModuleProvider: TestProjectModuleProvider by TestServices.testServiceAccessor()
