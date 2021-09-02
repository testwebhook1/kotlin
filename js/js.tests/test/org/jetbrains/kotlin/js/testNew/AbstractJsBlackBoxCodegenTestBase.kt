/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.testNew

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
import java.lang.Boolean.getBoolean

abstract class AbstractJsBlackBoxCodegenTestBase<R : ResultingArtifact.FrontendOutput<R>, I : ResultingArtifact.BackendInput<I>>(
    val targetFrontend: FrontendKind<R>,
    targetBackend: TargetBackend,
    private val pathToTestDir: String,
    private val testGroupOutputDirPrefix: String,
    private val skipMinification: Boolean = getBoolean("kotlin.js.skipMinificationTest"),
) : AbstractKotlinCompilerWithTargetBackendTest(targetBackend) {
    abstract val frontendFacade: Constructor<FrontendFacade<R>>
    abstract val frontendToBackendConverter: Constructor<Frontend2BackendConverter<R, I>>
    abstract val backendFacade: Constructor<BackendFacade<I, BinaryArtifacts.Js>>

    private val runTestInNashorn: Boolean = getBoolean("kotlin.js.useNashorn")

    override fun TestConfigurationBuilder.configuration() {
        globalDefaults {
            frontend = targetFrontend
            targetPlatform = JsPlatforms.defaultJsPlatform
            dependencyKind = DependencyKind.Binary
        }

        val pathToRootOutputDir = System.getProperty("kotlin.js.test.root.out.dir") ?: error("'kotlin.js.test.root.out.dir' is not set")
        defaultDirectives {
            JsEnvironmentConfigurationDirectives.PATH_TO_ROOT_OUTPUT_DIR with pathToRootOutputDir
            JsEnvironmentConfigurationDirectives.PATH_TO_TEST_DIR with pathToTestDir
            JsEnvironmentConfigurationDirectives.TEST_GROUP_OUTPUT_DIR_PREFIX with testGroupOutputDirPrefix
            +JsEnvironmentConfigurationDirectives.TYPED_ARRAYS
            +JsEnvironmentConfigurationDirectives.GENERATE_NODE_JS_RUNNER
            if (skipMinification) +JsEnvironmentConfigurationDirectives.SKIP_MINIFICATION
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
    pathToTestDir: String,
    testGroupOutputDirPrefix: String,
) : AbstractJsBlackBoxCodegenTestBase<ClassicFrontendOutputArtifact, ClassicBackendInput>(
    FrontendKinds.ClassicFrontend, TargetBackend.JS, pathToTestDir, testGroupOutputDirPrefix
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
            jsArtifactsHandlersStep {
                useHandlers(
                    ::JsAstHandler,
                    ::JsSourceMapHandler,
                    ::JsBoxRunner,
                    ::NodeJsGeneratorHandler,
                    ::JsMinifierRunner,
                )
            }
        }
    }
}

open class AbstractBoxJsTest : AbstractJsTest(pathToTestDir = "${TEST_DATA_DIR_PATH}/box/", testGroupOutputDirPrefix = "box/") {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.defaultDirectives {
            +JsEnvironmentConfigurationDirectives.RUN_MINIFIER_BY_DEFAULT
        }
    }
}

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
    testGroupOutputDirPrefix = "codegen/box/arrays-legacy-primitivearrays/"
) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.defaultDirectives {
            -JsEnvironmentConfigurationDirectives.TYPED_ARRAYS
        }
    }
}

open class AbstractSourceMapGenerationSmokeTest : AbstractJsTest(
    pathToTestDir = "${TEST_DATA_DIR_PATH}/sourcemap/",
    testGroupOutputDirPrefix = "sourcemap/"
) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.defaultDirectives {
            +JsEnvironmentConfigurationDirectives.GENERATE_SOURCE_MAP
            -JsEnvironmentConfigurationDirectives.GENERATE_NODE_JS_RUNNER
        }
    }
}

open class AbstractWebDemoExamples1Test : AbstractJsTest(
    pathToTestDir = "${TEST_DATA_DIR_PATH}/webDemoExamples1/",
    testGroupOutputDirPrefix = "webDemoExamples1/"
) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.defaultDirectives {
            -JsEnvironmentConfigurationDirectives.GENERATE_NODE_JS_RUNNER
        }
    }
}

open class AbstractWebDemoExamples2Test : AbstractJsTest(
    pathToTestDir = "${TEST_DATA_DIR_PATH}/webDemoExamples2/",
    testGroupOutputDirPrefix = "webDemoExamples2/"
) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.defaultDirectives {
            -JsEnvironmentConfigurationDirectives.GENERATE_NODE_JS_RUNNER
        }
    }
}

open class AbstractOutputPrefixPostfixTest : AbstractJsTest(
    pathToTestDir = "${TEST_DATA_DIR_PATH}/outputPrefixPostfix/",
    testGroupOutputDirPrefix = "outputPrefixPostfix/"
) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.defaultDirectives {
            -JsEnvironmentConfigurationDirectives.GENERATE_NODE_JS_RUNNER
        }
    }
}

open class AbstractMultiModuleOrderTest : AbstractJsTest(
    pathToTestDir = "${TEST_DATA_DIR_PATH}/multiModuleOrder/",
    testGroupOutputDirPrefix = "multiModuleOrder/"
)

open class AbstractLegacyJsTypeScriptExportTest : AbstractJsTest(
    pathToTestDir = "${TEST_DATA_DIR_PATH}/typescript-export/",
    testGroupOutputDirPrefix = "legacy-typescript-export/"
)
