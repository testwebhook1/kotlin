/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.directives

import org.jetbrains.kotlin.cli.common.arguments.K2JsArgumentConstants
import org.jetbrains.kotlin.js.config.ErrorTolerancePolicy
import org.jetbrains.kotlin.js.config.RuntimeDiagnostic
import org.jetbrains.kotlin.js.config.SourceMapSourceEmbedding
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.test.directives.model.DirectiveApplicability
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer

object JsEnvironmentConfigurationDirectives : SimpleDirectivesContainer() {
    val MODULE_KIND by enumDirective<ModuleKind>(
        description = "Specifies kind of js module",
        applicability = DirectiveApplicability.Any
    )

    val NO_JS_MODULE_SYSTEM by directive(
        description = "", // TODO
        applicability = DirectiveApplicability.Global
    )

    val INFER_MAIN_MODULE by directive(
        description = "", // TODO
        applicability = DirectiveApplicability.Global
    )

    val RUN_PLAIN_BOX_FUNCTION by directive(
        description = "", // TODO
        applicability = DirectiveApplicability.Global
    )

    val NO_INLINE by directive(
        description = "Disable inline in js module",
        applicability = DirectiveApplicability.Module
    )

    val SKIP_NODE_JS by directive(
        description = "", // TODO
        applicability = DirectiveApplicability.Global
    )

    val SKIP_MINIFICATION by directive(
        description = "", // TODO
        applicability = DirectiveApplicability.Global
    )

    val SKIP_SOURCEMAP_REMAPPING by directive(
        description = "", // TODO
        applicability = DirectiveApplicability.Global
    )

    val EXPECTED_REACHABLE_NODES by valueDirective(
        description = "", // TODO
        applicability = DirectiveApplicability.Global,
        parser = { it.toIntOrNull() }
    )

    val RECOMPILE by directive(
        description = "", // TODO
        applicability = DirectiveApplicability.File
    )

    val SOURCE_MAP_EMBED_SOURCES by enumDirective<SourceMapSourceEmbedding>(
        description = "", // TODO
        applicability = DirectiveApplicability.Module
    )

    val CALL_MAIN by directive(
        description = "", // TODO
        applicability = DirectiveApplicability.Global
    )

    val KJS_WITH_FULL_RUNTIME by directive(
        description = "", // TODO
        applicability = DirectiveApplicability.Global
    )

    val EXPECT_ACTUAL_LINKER by directive(
        description = "" // TODO
    )

    val SKIP_DCE_DRIVEN by directive(
        description = "", // TODO
        applicability = DirectiveApplicability.Global
    )

    val SPLIT_PER_MODULE by directive(
        description = "", // TODO
        applicability = DirectiveApplicability.Global
    )

    val SKIP_MANGLE_VERIFICATION by directive(
        description = "", // TODO
        applicability = DirectiveApplicability.Global
    )

    val ERROR_POLICY by enumDirective<ErrorTolerancePolicy>(
        description = "", // TODO
        applicability = DirectiveApplicability.Global,
        additionalParser = { ErrorTolerancePolicy.resolvePolicy(it) }
    )

    val PROPERTY_LAZY_INITIALIZATION by directive(
        description = "", // TODO
        applicability = DirectiveApplicability.Global
    )

    val SAFE_EXTERNAL_BOOLEAN by directive(
        description = "", // TODO
        applicability = DirectiveApplicability.Global
    )

    val SAFE_EXTERNAL_BOOLEAN_DIAGNOSTIC by enumDirective<RuntimeDiagnostic>(
        description = "", // TODO
        applicability = DirectiveApplicability.Global,
        additionalParser = {
            when (it.lowercase()) {
                K2JsArgumentConstants.RUNTIME_DIAGNOSTIC_LOG -> RuntimeDiagnostic.LOG
                K2JsArgumentConstants.RUNTIME_DIAGNOSTIC_EXCEPTION -> RuntimeDiagnostic.EXCEPTION
                else -> null
            }
        }
    )

    val DONT_RUN_GENERATED_CODE by stringDirective(
        description = "", // TODO
        applicability = DirectiveApplicability.Global,
    )

    // -----

    val PATH_TO_TEST_DIR by stringDirective(
        description = "Specify the path to directory with test files. " +
                "This path is used to copy hierarchy from test file to test dir and use the same hierarchy in output dir.",
        applicability = DirectiveApplicability.Global
    )

    val PATH_TO_ROOT_OUTPUT_DIR by stringDirective(
        description = "",  // TODO
        applicability = DirectiveApplicability.Global
    )

    val TEST_GROUP_OUTPUT_DIR_PREFIX by stringDirective(
        description = "", // TODO
        applicability = DirectiveApplicability.Global
    )

    val DUMP_ARTIFACTS_TO_OUTPUT_DIR by directive(
        description = "", // TODO
        applicability = DirectiveApplicability.Global
    )

    val TYPED_ARRAYS by directive(
        description = "", // TODO
        applicability = DirectiveApplicability.Global
    )

    val GENERATE_SOURCE_MAP by directive(
        description = "", // TODO
        applicability = DirectiveApplicability.Global
    )

    val GENERATE_NODE_JS_RUNNER by directive(
        description = "", // TODO
        applicability = DirectiveApplicability.Global
    )

    val RUN_MINIFIER_BY_DEFAULT by directive(
        description = "", // TODO
        applicability = DirectiveApplicability.Global
    )

    val SKIP_REGULAR_MODE by directive(
        description = "", // TODO
        applicability = DirectiveApplicability.Global
    )

    val RUN_IR_DCE by directive(
        description = "", // TODO
        applicability = DirectiveApplicability.Global
    )

    val RUN_IR_PIR by directive(
        description = "", // TODO
        applicability = DirectiveApplicability.Global
    )
}
