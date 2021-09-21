/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.fir.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.analysis.api.fir.test.framework.AbstractHLApiSingleFileTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.base.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.api.analyse
import org.jetbrains.kotlin.analysis.api.symbols.markers.*
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractCompileTimeConstantEvaluatorTest : AbstractHLApiSingleFileTest() {
    override fun doTestByFileStructure(ktFile: KtFile, module: TestModule, testServices: TestServices) {
        val element = testServices.expressionMarkerProvider.getSelectedElement(ktFile)
        val expression = when (element) {
            is KtExpression -> element
            is KtValueArgument -> element.getArgumentExpression()
            else -> null
        } ?: testServices.assertions.fail { "Unsupported expression: $element" }
        val constantValue = executeOnPooledThreadInReadAction {
            analyse(expression) { expression.evaluate() }
        }
        val actual = buildString {
            appendLine("expression: ${expression.text}")
            appendLine("constant_value: ${analyse(ktFile) { constantValue?.stringRepresentation() }}")
            appendLine("constant: ${(constantValue as? KtLiteralConstantValue<*>)?.toConst()}")
        }
        testServices.assertions.assertEqualsToFile(testDataFileSibling(".txt"), actual)
    }

    private fun KtConstantValue.stringRepresentation(): String {
        return when (this) {
            is KtArrayConstantValue -> buildString {
                appendLine("KtArrayConstantValue [")
                appendLine(INDENT, values.joinToString(separator = "\n") { it.stringRepresentation() })
                append("]")
            }
            is KtAnnotationConstantValue -> buildString {
                append("KtAnnotationConstantValue(")
                append(classId?.relativeClassName)
                append(", ")
                arguments.joinTo(this, separator = ", ", prefix = "(", postfix = ")") {
                    "${it.name} = ${it.expression.stringRepresentation()}"
                }
                append(")")
            }
            is KtEnumEntryConstantValue -> buildString {
                append("KtEnumEntryConstantValue(")
                append("$callableId")
                append(")")
            }
            is KtLiteralConstantValue<*> -> toString()
            is KtUnsupportedConstantValue -> "KtUnsupportedConstantValue"
        }
    }

    private fun StringBuilder.appendLine(indent: Int, value: String) {
        appendLine(value.prependIndent(" ".repeat(indent)))
    }
}

private const val INDENT = 2
