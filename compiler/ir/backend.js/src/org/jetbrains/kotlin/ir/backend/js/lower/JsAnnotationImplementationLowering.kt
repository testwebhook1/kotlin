/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.backend.common.lower.AnnotationImplementationTransformer
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.builders.IrBlockBodyBuilder
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isArray
import org.jetbrains.kotlin.ir.util.isAnnotationClass
import org.jetbrains.kotlin.ir.util.isPrimitiveArray
import org.jetbrains.kotlin.ir.util.render


// JS PIR (and IC) requires DeclarationTransformer instead of FileLoweringPass
class JsAnnotationImplementationTransformer(val jsContext: JsIrBackendContext) :
    AnnotationImplementationTransformer(jsContext, null),
    DeclarationTransformer {

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? =
        if (declaration is IrClass && declaration.isAnnotationClass) listOf(visitClassNew(declaration))
        else null

    override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
        // No-op
        return expression
    }

    override fun visitClassNew(declaration: IrClass): IrClass {
        if (!declaration.isAnnotationClass) return declaration
        context.irFactory.stageController.unrestrictDeclarationListsAccess {
            implementGeneratedFunctions(declaration, declaration, declaration.createGeneratedFunctions())
        }
        return declaration
    }

    private val arraysContentEquals: Map<IrType, IrSimpleFunctionSymbol> =
        requireNotNull(jsContext.ir.symbols.arraysContentEquals) { "contentEquals symbols should be defined in JS IR context" }

    override fun getArrayContentEqualsSymbol(type: IrType) =
        when {
            type.isPrimitiveArray() -> arraysContentEquals[type]
            else -> arraysContentEquals.entries.singleOrNull { (k, _) -> k.isArray() }?.value
        } ?: error("Can't find an Arrays.contentEquals method for array type ${type.render()}")

    override fun getEqualsProperties(annotationClass: IrClass, implClass: IrClass) = annotationClass.getAnnotationProperties()
    override fun getHashCodeProperties(annotationClass: IrClass, implClass: IrClass) = annotationClass.getAnnotationProperties()
    override fun getToStringProperties(annotationClass: IrClass, implClass: IrClass) = annotationClass.getAnnotationProperties()

    override fun implementAnnotationPropertiesAndConstructor(
        implClass: IrClass,
        annotationClass: IrClass,
        generatedConstructor: IrConstructor
    ) {
        throw IllegalStateException("Should not be called")
    }
}