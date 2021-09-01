/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.testNew

import org.jetbrains.kotlin.js.test.BasicBoxTest
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.classic.ClassicBackendInput
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.jsArtifactsHandlersStep
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontend2ClassicBackendConverter
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendFacade
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendOutputArtifact
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator.Companion.TEST_DATA_DIR_PATH

abstract class AbstractJsBlackBoxCodegenTestBase<R : ResultingArtifact.FrontendOutput<R>, I : ResultingArtifact.BackendInput<I>>(
    val targetFrontend: FrontendKind<R>,
    targetBackend: TargetBackend
) : AbstractKotlinCompilerWithTargetBackendTest(targetBackend) {
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
//        useAfterAnalysisCheckers(::BlackBoxCodegenSuppressor) TODO uncomment later
    }
}

abstract class AbstractJsTest(
    private val pathToTestDir: String,
    private val testGroupOutputDirPrefix: String,
    private val typedArraysEnabled: Boolean = true,
    private val generateSourceMap: Boolean = false,
    private val generateNodeJsRunner: Boolean = true,
) : AbstractJsBlackBoxCodegenTestBase<ClassicFrontendOutputArtifact, ClassicBackendInput>(FrontendKinds.ClassicFrontend, TargetBackend.JS) {
    override val frontendFacade: Constructor<FrontendFacade<ClassicFrontendOutputArtifact>>
        get() = ::ClassicFrontendFacade

    override val frontendToBackendConverter: Constructor<Frontend2BackendConverter<ClassicFrontendOutputArtifact, ClassicBackendInput>>
        get() = ::ClassicFrontend2ClassicBackendConverter

    override val backendFacade: Constructor<BackendFacade<ClassicBackendInput, BinaryArtifacts.Js>>
        get() = ::ClassicJsBackendFacade

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            with(builder) {
                defaultDirectives {
                    JsEnvironmentConfigurationDirectives.PATH_TO_TEST_DIR with pathToTestDir
                    JsEnvironmentConfigurationDirectives.TEST_GROUP_OUTPUT_DIR_PREFIX with testGroupOutputDirPrefix
                    if (typedArraysEnabled) +JsEnvironmentConfigurationDirectives.TYPED_ARRAYS
                    if (generateNodeJsRunner) +JsEnvironmentConfigurationDirectives.GENERATE_NODE_JS_RUNNER
                    if (generateSourceMap) +JsEnvironmentConfigurationDirectives.GENERATE_SOURCE_MAP
                }
            }

            jsArtifactsHandlersStep {
                useHandlers(
                    ::JsAstHandler,
                    ::JsSourceMapHandler,
                    ::JsBoxRunner,
                )
            }
        }
    }
}

open class AbstractBoxJsTest : AbstractJsTest(
    pathToTestDir = "${TEST_DATA_DIR_PATH}box/",
    testGroupOutputDirPrefix = "box/"
)

open class AbstractJsCodegenBoxTest : AbstractJsTest(
    pathToTestDir = "compiler/testData/codegen/box/",
    testGroupOutputDirPrefix = "codegen/box/"
)

open class AbstractJsCodegenInlineTest : AbstractJsTest(
    pathToTestDir = "compiler/testData/codegen/boxInline",
    testGroupOutputDirPrefix = "codegen/boxInline"
)

open class AbstractJsLegacyPrimitiveArraysBoxTest : AbstractJsTest(
    pathToTestDir = "compiler/testData/codegen/box/arrays/",
    testGroupOutputDirPrefix = "codegen/box/arrays-legacy-primitivearrays/",
    typedArraysEnabled = false
)

open class AbstractSourceMapGenerationSmokeTest : AbstractJsTest(
    pathToTestDir = "${TEST_DATA_DIR_PATH}sourcemap/",
    testGroupOutputDirPrefix = "sourcemap/",
    generateSourceMap = true,
    generateNodeJsRunner = false
)

open class AbstractWebDemoExamples1Test : AbstractJsTest(
    pathToTestDir = "${TEST_DATA_DIR_PATH}webDemoExamples1/",
    testGroupOutputDirPrefix = "webDemoExamples1/",
    generateNodeJsRunner = false
)

open class AbstractWebDemoExamples2Test : AbstractJsTest(
    pathToTestDir = "${TEST_DATA_DIR_PATH}webDemoExamples2/",
    testGroupOutputDirPrefix = "webDemoExamples2/",
    generateNodeJsRunner = false
)

open class AbstractOutputPrefixPostfixTest : AbstractJsTest(
    pathToTestDir = "${TEST_DATA_DIR_PATH}outputPrefixPostfix/",
    testGroupOutputDirPrefix = "outputPrefixPostfix/",
    generateNodeJsRunner = false
)

open class AbstractMultiModuleOrderTest : AbstractJsTest(
    pathToTestDir = "${TEST_DATA_DIR_PATH}multiModuleOrder/",
    testGroupOutputDirPrefix = "multiModuleOrder/"
)

open class AbstractLegacyJsTypeScriptExportTest : AbstractJsTest(
    pathToTestDir = "${TEST_DATA_DIR_PATH}typescript-export/",
    testGroupOutputDirPrefix = "legacy-typescript-export/"
)
