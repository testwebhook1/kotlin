/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.testNew

import org.jetbrains.kotlin.js.test.*
import org.jetbrains.kotlin.js.testNew.utils.*
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.handlers.JsBinaryArtifactHandler
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives.DONT_RUN_GENERATED_CODE
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives.RUN_IR_DCE
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives.RUN_IR_PIR
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives.SKIP_DCE_DRIVEN
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives.SKIP_REGULAR_MODE
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.defaultsProvider
import org.jetbrains.kotlin.test.services.moduleStructure

class JsBoxRunner(testServices: TestServices) : JsBinaryArtifactHandler(testServices) {
    private val runTestInNashorn: Boolean = java.lang.Boolean.getBoolean("kotlin.js.useNashorn")
    val modulesToArtifact = mutableMapOf<TestModule, BinaryArtifacts.Js>()

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (someAssertionWasFailed) return

        val globalDirectives = testServices.moduleStructure.allDirectives
        val dontRunGeneratedCode = globalDirectives[DONT_RUN_GENERATED_CODE]
            .contains(testServices.defaultsProvider.defaultTargetBackend?.name)

        if (dontRunGeneratedCode) return

        val (allJsFiles, dceAllJsFiles, pirAllJsFiles) = getAllFilesForRunner(testServices, modulesToArtifact)

        val withModuleSystem = testWithModuleSystem(testServices)
        val testModuleName = getTestModuleName(testServices)
        val testPackage = extractTestPackage(testServices)

        val dontSkipRegularMode = SKIP_REGULAR_MODE !in globalDirectives
        val dontSkipDceDriven = SKIP_DCE_DRIVEN !in globalDirectives
        val runIrDce = RUN_IR_DCE in globalDirectives
        val runIrPir = RUN_IR_PIR in globalDirectives
        if (dontSkipRegularMode) {
            runGeneratedCode(allJsFiles, testModuleName, testPackage, DEFAULT_EXPECTED_RESULT, withModuleSystem)

            if (runIrDce) {
                runGeneratedCode(dceAllJsFiles, testModuleName, testPackage, DEFAULT_EXPECTED_RESULT, withModuleSystem)
            }
        }

        if (runIrPir && dontSkipDceDriven) {
            runGeneratedCode(pirAllJsFiles, testModuleName, testPackage, DEFAULT_EXPECTED_RESULT, withModuleSystem)
        }
    }

    override fun processModule(module: TestModule, info: BinaryArtifacts.Js) {
        modulesToArtifact[module] = info
    }

    private fun runGeneratedCode(
        jsFiles: List<String>,
        testModuleName: String?,
        testPackage: String?,
        expectedResult: String,
        withModuleSystem: Boolean
    ) {
        getTestChecker()
            .check(jsFiles, testModuleName, testPackage, JsEnvironmentConfigurator.TEST_FUNCTION, expectedResult, withModuleSystem)
    }

    private fun getTestChecker(): AbstractJsTestChecker {
        val targetBackend = testServices.defaultsProvider.defaultTargetBackend ?: TargetBackend.JS
        return if (targetBackend.isIR) {
            if (runTestInNashorn) NashornIrJsTestChecker else V8IrJsTestChecker
        } else {
            if (runTestInNashorn) NashornJsTestChecker else V8JsTestChecker
        }
    }

    companion object {
        private const val DEFAULT_EXPECTED_RESULT = "OK"
    }
}