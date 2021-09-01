/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.testNew

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.js.JavaScript
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.test.backend.handlers.JsBinaryArtifactHandler
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator.Companion.TEST_DATA_DIR_PATH
import org.jetbrains.kotlin.test.services.isJsFile
import org.jetbrains.kotlin.test.services.moduleStructure
import java.io.File

class JsBoxRunner(testServices: TestServices) : JsBinaryArtifactHandler(testServices) {
    val moduleToArtifact = mutableMapOf<TestModule, BinaryArtifacts.Js>()

    companion object {
        private const val MODULE_EMULATION_FILE = "$TEST_DATA_DIR_PATH/moduleEmulation.js"
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (someAssertionWasFailed) return
        val originalFile = testServices.moduleStructure.originalTestDataFiles.first()
        val outputDir = JsEnvironmentConfigurator.getJsArtifactsOutputDir(testServices)
        val dceOutputDir = JsEnvironmentConfigurator.getDceJsArtifactsOutputDir(testServices)
        val pirOutputDir = JsEnvironmentConfigurator.getPirJsArtifactsOutputDir(testServices)

        val commonFiles = JsAdditionalSourceProvider.getAdditionalJsFiles(originalFile.parent).map { it.absolutePath }
        val inputJsFiles = moduleToArtifact
            .flatMap { moduleToArtifact -> moduleToArtifact.key.files.map { moduleToArtifact.key to it } }
            .filter { it.second.isJsFile }
            .map { (module, inputJsFile) ->
                val sourceFile = File(inputJsFile.name)
                val newName = JsEnvironmentConfigurator.getJsArtifactSimpleName(testServices, module.name) + "-js-" + sourceFile.name
                val targetFile = File(outputDir, newName)
                FileUtil.copy(File(inputJsFile.name), targetFile)
                targetFile.absolutePath
            }

        val globalDirectives = testServices.moduleStructure.allDirectives
        val globalModuleKind = globalDirectives[JsEnvironmentConfigurationDirectives.MODULE_KIND].singleOrNull() ?: ModuleKind.PLAIN
        val withModuleSystem = globalModuleKind != ModuleKind.PLAIN && JsEnvironmentConfigurationDirectives.NO_MODULE_SYSTEM_PATTERN !in globalDirectives

        val additionalFiles = mutableListOf<String>()
        if (withModuleSystem) additionalFiles += MODULE_EMULATION_FILE

        originalFile.parentFile.resolve(originalFile.nameWithoutExtension + JavaScript.DOT_EXTENSION)
            .takeIf { it.exists() }
            ?.let { additionalFiles += it.absolutePath }

        originalFile.parentFile.resolve(originalFile.nameWithoutExtension + "__main.js")
            .takeIf { it.exists() }
            ?.let { additionalFiles += it.absolutePath }

        val artifactsPaths = moduleToArtifact.values.map { it.outputFile.absolutePath }
        val allCommonFiles = additionalFiles + inputJsFiles + commonFiles
        val allJsFiles = allCommonFiles + artifactsPaths
        val dceAllJsFiles = allCommonFiles + artifactsPaths.map { it.replace(outputDir.absolutePath, dceOutputDir.absolutePath) }
        val pirAllJsFiles = allCommonFiles + artifactsPaths.map { it.replace(outputDir.absolutePath, pirOutputDir.absolutePath) }


        // TODO
    }

    override fun processModule(module: TestModule, info: BinaryArtifacts.Js) {
        moduleToArtifact[module] = info
    }

    private fun getMainModule(): TestModule {
        val modules = testServices.moduleStructure.modules
        val inferMainModule = JsEnvironmentConfigurationDirectives.INFER_MAIN_MODULE in testServices.moduleStructure.allDirectives
        return when {
            inferMainModule -> modules.last()
            else -> modules.singleOrNull { it.name == JsEnvironmentConfigurator.TEST_MODULE_NAME }
                ?: modules.single { it.name == JsEnvironmentConfigurator.DEFAULT_MODULE_NAME }
        }
    }

    private fun createNodeJsRunner() {

    }
}