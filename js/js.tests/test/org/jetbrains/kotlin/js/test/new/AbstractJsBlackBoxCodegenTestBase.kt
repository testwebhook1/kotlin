/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.new

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.common.output.writeAllTo
import org.jetbrains.kotlin.js.analyzer.JsAnalysisResult
import org.jetbrains.kotlin.js.facade.K2JSTranslator
import org.jetbrains.kotlin.js.facade.MainCallParameters
import org.jetbrains.kotlin.js.facade.TranslationResult
import org.jetbrains.kotlin.js.facade.TranslationUnit
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.classic.ClassicBackendFacade
import org.jetbrains.kotlin.test.backend.classic.ClassicBackendInput
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.jsArtifactsHandlersStep
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontend2ClassicBackendConverter
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendFacade
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendOutputArtifact
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator.Companion.outputFilePath
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.services.temporaryDirectoryManager
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.nio.charset.Charset

abstract class AbstractJsBlackBoxCodegenTestBase<R : ResultingArtifact.FrontendOutput<R>, I : ResultingArtifact.BackendInput<I>>(
    val targetFrontend: FrontendKind<R>,
    targetBackend: TargetBackend
) : AbstractKotlinCompilerWithTargetBackendTest(targetBackend) {
    companion object {
        const val TEST_DATA_DIR_PATH = "js/js.translator/testData/"
        const val DIST_DIR_JS_PATH = "dist/js/"
    }

    abstract val frontendFacade: Constructor<FrontendFacade<R>>
    abstract val frontendToBackendConverter: Constructor<Frontend2BackendConverter<R, I>>
    abstract val backendFacade: Constructor<BackendFacade<I, BinaryArtifacts.Js>>

    override fun TestConfigurationBuilder.configuration() {
        globalDefaults {
            frontend = targetFrontend
            targetPlatform = JsPlatforms.defaultJsPlatform
            dependencyKind = DependencyKind.Binary
        }

        val pathToRootOutputDir = System.getProperty("kotlin.js.test.root.out.dir") ?: error("'kotlin.js.test.root.out.dir' is not set")
        defaultDirectives {
            JsEnvironmentConfigurationDirectives.PATH_TO_ROOT_OUTPUT_DIR with pathToRootOutputDir
        }

        facadeStep(frontendFacade)
        facadeStep(frontendToBackendConverter)
        facadeStep(backendFacade)

        useConfigurators(
            ::CommonEnvironmentConfigurator,
            ::JsEnvironmentConfigurator,
        )

        useAdditionalSourceProviders(
            ::JsAdditionalSourceProvider
        )

        jsArtifactsHandlersStep {
            useHandlers(
                ::JsBoxRunner
            )
        }
//        useAfterAnalysisCheckers(::BlackBoxCodegenSuppressor) TODO uncomment later
    }
}

open class AbstractJsBlackBoxCodegenTest : AbstractJsBlackBoxCodegenTestBase<ClassicFrontendOutputArtifact, ClassicBackendInput>(
    FrontendKinds.ClassicFrontend,
    TargetBackend.JS
) {
    override val frontendFacade: Constructor<FrontendFacade<ClassicFrontendOutputArtifact>>
        get() = ::ClassicFrontendFacade

    override val frontendToBackendConverter: Constructor<Frontend2BackendConverter<ClassicFrontendOutputArtifact, ClassicBackendInput>>
        get() = ::ClassicFrontend2ClassicBackendConverter

    override val backendFacade: Constructor<BackendFacade<ClassicBackendInput, BinaryArtifacts.Js>>
        get() = ::ClassicJsBackendFacade

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            forTestsMatching(TEST_DATA_DIR_PATH + "box/*") {
                defaultDirectives {
                    JsEnvironmentConfigurationDirectives.PATH_TO_TEST_DIR with "${TEST_DATA_DIR_PATH}box/"
                    JsEnvironmentConfigurationDirectives.TEST_GROUP_OUTPUT_DIR_PREFIX with "box/"
                }
            }

            forTestsMatching("compiler/testData/codegen/box/*") {
                defaultDirectives {
                    JsEnvironmentConfigurationDirectives.PATH_TO_TEST_DIR with "compiler/testData/codegen/box/"
                    JsEnvironmentConfigurationDirectives.TEST_GROUP_OUTPUT_DIR_PREFIX with "codegen/box/"
                }
            }

            forTestsMatching("compiler/testData/codegen/boxInline/*") {
                defaultDirectives {
                    JsEnvironmentConfigurationDirectives.PATH_TO_TEST_DIR with "compiler/testData/codegen/boxInline"
                    JsEnvironmentConfigurationDirectives.TEST_GROUP_OUTPUT_DIR_PREFIX with "codegen/boxInline"
                }
            }

            forTestsMatching("compiler/testData/codegen/box/arrays/*") {
                defaultDirectives {
                    JsEnvironmentConfigurationDirectives.PATH_TO_TEST_DIR with "compiler/testData/codegen/box/arrays/"
                    JsEnvironmentConfigurationDirectives.TEST_GROUP_OUTPUT_DIR_PREFIX with "codegen/box/arrays-legacy-primitivearrays/"
                }
            }

            forTestsMatching(TEST_DATA_DIR_PATH + "sourcemap/*") {
                defaultDirectives {
                    JsEnvironmentConfigurationDirectives.PATH_TO_TEST_DIR with "${TEST_DATA_DIR_PATH}sourcemap/"
                    JsEnvironmentConfigurationDirectives.TEST_GROUP_OUTPUT_DIR_PREFIX with "sourcemap/"
                }
            }

            forTestsMatching(TEST_DATA_DIR_PATH + "outputPrefixPostfix/*") {
                defaultDirectives {
                    JsEnvironmentConfigurationDirectives.PATH_TO_TEST_DIR with "${TEST_DATA_DIR_PATH}outputPrefixPostfix/"
                    JsEnvironmentConfigurationDirectives.TEST_GROUP_OUTPUT_DIR_PREFIX with "outputPrefixPostfix/"
                }
            }

            forTestsMatching(TEST_DATA_DIR_PATH + "multiModuleOrder/*") {
                defaultDirectives {
                    JsEnvironmentConfigurationDirectives.PATH_TO_TEST_DIR with "${TEST_DATA_DIR_PATH}multiModuleOrder/"
                    JsEnvironmentConfigurationDirectives.TEST_GROUP_OUTPUT_DIR_PREFIX with "multiModuleOrder/"
                }
            }

            forTestsMatching(TEST_DATA_DIR_PATH + "typescript-export/*") {
                defaultDirectives {
                    JsEnvironmentConfigurationDirectives.PATH_TO_TEST_DIR with "${TEST_DATA_DIR_PATH}typescript-export/"
                    JsEnvironmentConfigurationDirectives.TEST_GROUP_OUTPUT_DIR_PREFIX with "legacy-typescript-export/"
                }
            }
        }
    }
}

class ClassicJsBackendFacade(
    testServices: TestServices
) : ClassicBackendFacade<BinaryArtifacts.Js>(testServices, ArtifactKinds.Js) {
    companion object {
        const val KOTLIN_TEST_INTERNAL = "\$kotlin_test_internal\$"
    }

    private fun getOutputDir(file: File, testGroupOutputDir: File, stopFile: File): File {
        return generateSequence(file.parentFile) { it.parentFile }
            .takeWhile { it != stopFile }
            .map { it.name }
            .toList().asReversed()
            .fold(testGroupOutputDir, ::File)
    }

    private fun wrapWithModuleEmulationMarkers(content: String, moduleKind: ModuleKind, moduleId: String): String {
        val escapedModuleId = StringUtil.escapeStringCharacters(moduleId)

        return when (moduleKind) {
            ModuleKind.COMMON_JS -> "$KOTLIN_TEST_INTERNAL.beginModule();\n" +
                    "$content\n" +
                    "$KOTLIN_TEST_INTERNAL.endModule(\"$escapedModuleId\");"

            ModuleKind.AMD, ModuleKind.UMD ->
                "if (typeof $KOTLIN_TEST_INTERNAL !== \"undefined\") { " +
                        "$KOTLIN_TEST_INTERNAL.setModuleId(\"$escapedModuleId\"); }\n" +
                        "$content\n"

            ModuleKind.PLAIN -> content
        }
    }

    override fun transform(module: TestModule, inputArtifact: ClassicBackendInput): BinaryArtifacts.Js? {
        if (module.name.endsWith(JsEnvironmentConfigurator.OLD_MODULE_SUFFIX)) return null
        val originalFile = module.files.first().originalFile
//        val stopFile = File(module.directives[JsEnvironmentConfigurationDirectives.PATH_TO_TEST_DIR].single())
//        val pathToRootOutputDir = module.directives[JsEnvironmentConfigurationDirectives.PATH_TO_ROOT_OUTPUT_DIR].single()
//        val testGroupOutputDirPrefix = module.directives[JsEnvironmentConfigurationDirectives.TEST_GROUP_OUTPUT_DIR_PREFIX].single()

//        val testGroupOutputDirForCompilation = File(pathToRootOutputDir + "out/" + testGroupOutputDirPrefix)
//        val testGroupOutputDirForMinification = File(pathToRootOutputDir + "out-min/" + testGroupOutputDirPrefix)
//        val testGroupOutputDirForPir = File(pathToRootOutputDir + "out-pir/" + testGroupOutputDirPrefix)
//
//        val outputDir = getOutputDir(originalFile, testGroupOutputDirForCompilation, stopFile)
//        val dceOutputDir = getOutputDir(originalFile, testGroupOutputDirForMinification, stopFile)
//        val pirOutputDir = getOutputDir(originalFile, testGroupOutputDirForPir, stopFile)
//
//        val outputFileName = module.outputFileName(outputDir) + ".js"
//        val dceOutputFileName = module.outputFileName(dceOutputDir) + ".js"
//        val pirOutputFileName = module.outputFileName(pirOutputDir) + ".js"
////        val abiVersion = module.abiVersion
////        val isMainModule = mainModuleName == module.name

        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
        val (psiFiles, analysisResult, project, _) = inputArtifact

        // TODO how to reuse this config from frontend
        val jsConfig = JsEnvironmentConfigurator.createJsConfig(project, configuration)
        val units = psiFiles.map(TranslationUnit::SourceFile)
        val mainCallParameters = when (JsEnvironmentConfigurationDirectives.CALL_MAIN_PATTERN) {
            in module.directives -> MainCallParameters.mainWithArguments(listOf("testArg"))
            else -> MainCallParameters.noCall()
        }

        val translator = K2JSTranslator(jsConfig, false)
        val translationResult = translator.translateUnits(
            JsEnvironmentConfigurator.Companion.ExceptionThrowingReporter, units, mainCallParameters, analysisResult as? JsAnalysisResult
        )

        // TODO is this correct way to report errors?
        if (translationResult !is TranslationResult.Success) {
            val outputStream = ByteArrayOutputStream()
            val collector = PrintingMessageCollector(PrintStream(outputStream), MessageRenderer.PLAIN_FULL_PATHS, true)
            AnalyzerWithCompilerReport.reportDiagnostics(translationResult.diagnostics, collector)
            val messages = outputStream.toByteArray().toString(Charset.forName("UTF-8"))
            throw AssertionError("The following errors occurred compiling test:\n" + messages)
        }

        val outputDir = testServices.temporaryDirectoryManager.getOrCreateTempDirectory(JsEnvironmentConfigurator.OUTPUT_DIR_NAME)
        val outputFile = File(testServices.moduleStructure.outputFilePath(outputDir, module.name))
        val outputPrefixFile = originalFile.parentFile.resolve(originalFile.name + ".prefix").takeIf { it.exists() }
        val outputPostfixFile = originalFile.parentFile.resolve(originalFile.name + ".postfix").takeIf { it.exists() }
        val outputFiles = translationResult.getOutputFiles(outputFile, outputPrefixFile, outputPostfixFile)
        outputFiles.writeAllTo(outputDir)

        if (jsConfig.moduleKind != ModuleKind.PLAIN) {
            val content = FileUtil.loadFile(outputFile, true)
            val wrappedContent = wrapWithModuleEmulationMarkers(content, moduleId = jsConfig.moduleId, moduleKind = jsConfig.moduleKind)
            FileUtil.writeToFile(outputFile, wrappedContent)
        }

        return BinaryArtifacts.OldJsArtifact(/*outputFile.readText()*/translationResult.program)
    }
}

