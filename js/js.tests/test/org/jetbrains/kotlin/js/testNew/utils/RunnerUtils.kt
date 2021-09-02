/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.testNew.utils

import org.jetbrains.kotlin.js.JavaScript
import org.jetbrains.kotlin.js.test.BasicBoxTest
import org.jetbrains.kotlin.js.testNew.JsAdditionalSourceProvider
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator.Companion.TEST_FUNCTION
import java.io.File

private const val MODULE_EMULATION_FILE = "${JsEnvironmentConfigurator.TEST_DATA_DIR_PATH}/moduleEmulation.js"

private fun extractJsFiles(testServices: TestServices, modulesToArtifact: Map<TestModule, BinaryArtifacts.Js>): List<String> {
    val outputDir = JsEnvironmentConfigurator.getJsArtifactsOutputDir(testServices)
    return modulesToArtifact
        .flatMap { moduleToArtifact -> moduleToArtifact.key.files.map { moduleToArtifact.key to it } }
        .filter { it.second.isJsFile }
        .map { (module, inputJsFile) ->
            val newName = JsEnvironmentConfigurator.getJsArtifactSimpleName(testServices, module.name) + "-js-" + inputJsFile.name
            val targetFile = File(outputDir, newName)
            targetFile.writeText(inputJsFile.originalContent)
            targetFile.absolutePath
        }
}

private fun getAdditionalFiles(testServices: TestServices): List<String> {
    val originalFile = testServices.moduleStructure.originalTestDataFiles.first()

    val withModuleSystem = testWithModuleSystem(testServices)

    val additionalFiles = mutableListOf<String>()
    if (withModuleSystem) additionalFiles += MODULE_EMULATION_FILE

    originalFile.parentFile.resolve(originalFile.nameWithoutExtension + JavaScript.DOT_EXTENSION)
        .takeIf { it.exists() }
        ?.let { additionalFiles += it.absolutePath }

    originalFile.parentFile.resolve(originalFile.nameWithoutExtension + "__main.js")
        .takeIf { it.exists() }
        ?.let { additionalFiles += it.absolutePath }

    return additionalFiles
}

fun testWithModuleSystem(testServices: TestServices): Boolean {
    val globalDirectives = testServices.moduleStructure.allDirectives
    val globalModuleKind = globalDirectives[JsEnvironmentConfigurationDirectives.MODULE_KIND].singleOrNull() ?: ModuleKind.PLAIN
    return globalModuleKind != ModuleKind.PLAIN && JsEnvironmentConfigurationDirectives.NO_MODULE_SYSTEM_PATTERN !in globalDirectives
}

fun getAllFilesForRunner(
    testServices: TestServices, modulesToArtifact: Map<TestModule, BinaryArtifacts.Js>
): Triple<List<String>, List<String>, List<String>> {
    val originalFile = testServices.moduleStructure.originalTestDataFiles.first()
    val outputDir = JsEnvironmentConfigurator.getJsArtifactsOutputDir(testServices)
    val dceOutputDir = JsEnvironmentConfigurator.getDceJsArtifactsOutputDir(testServices)
    val pirOutputDir = JsEnvironmentConfigurator.getPirJsArtifactsOutputDir(testServices)

    val commonFiles = JsAdditionalSourceProvider.getAdditionalJsFiles(originalFile.parent).map { it.absolutePath }
    val inputJsFiles = extractJsFiles(testServices, modulesToArtifact)

    val additionalFiles = getAdditionalFiles(testServices)
    val artifactsPaths = modulesToArtifact.values.map { it.outputFile.absolutePath }
    val allCommonFiles = additionalFiles + inputJsFiles + commonFiles

    val allJsFiles = allCommonFiles + artifactsPaths
    val dceAllJsFiles = allCommonFiles + artifactsPaths.map { it.replace(outputDir.absolutePath, dceOutputDir.absolutePath) }
    val pirAllJsFiles = allCommonFiles + artifactsPaths.map { it.replace(outputDir.absolutePath, pirOutputDir.absolutePath) }

    return Triple(allJsFiles, dceAllJsFiles, pirAllJsFiles)
}

fun getOnlyJsFilesForRunner(testServices: TestServices, modulesToArtifact: Map<TestModule, BinaryArtifacts.Js>): List<String> {
    return getAllFilesForRunner(testServices, modulesToArtifact).first
}

private fun getMainModule(testServices: TestServices): TestModule {
    val modules = testServices.moduleStructure.modules
    val inferMainModule = JsEnvironmentConfigurationDirectives.INFER_MAIN_MODULE in testServices.moduleStructure.allDirectives
    return when {
        inferMainModule -> modules.last()
        else -> modules.singleOrNull { it.name == JsEnvironmentConfigurator.TEST_MODULE_NAME }
            ?: modules.single { it.name == JsEnvironmentConfigurator.DEFAULT_MODULE_NAME }
    }
}

fun getMainModuleName(testServices: TestServices): String {
    return getMainModule(testServices).name
}

fun getTestModuleName(testServices: TestServices): String? {
    val runPlainBoxFunction = JsEnvironmentConfigurationDirectives.RUN_PLAIN_BOX_FUNCTION in testServices.moduleStructure.allDirectives
    if (runPlainBoxFunction) return null
    return getMainModule(testServices).name
}

fun extractTestPackage(testServices: TestServices): String? {
    val runPlainBoxFunction = JsEnvironmentConfigurationDirectives.RUN_PLAIN_BOX_FUNCTION in testServices.moduleStructure.allDirectives
    if (runPlainBoxFunction) return null
    val ktFiles = testServices.moduleStructure.modules.flatMap { module ->
        module.files
            .filter { it.isKtFile }
            .map {
                val project = testServices.compilerConfigurationProvider.getProject(module)
                testServices.sourceFileProvider.getKtFileForSourceFile(it, project)
            }
    }

    return ktFiles.single { ktFile ->
        val boxFunction = ktFile.declarations.find { it is KtNamedFunction && it.name == TEST_FUNCTION }
        boxFunction != null
    }.packageFqName.asString().takeIf { it.isNotEmpty() }
}
