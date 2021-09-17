/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.llvm.objc

import llvm.LLVMTypeRef
import llvm.LLVMValueRef
import org.jetbrains.kotlin.backend.konan.llvm.*

internal open class ObjCCodeGenerator(val codegen: CodeGenerator) {
    val context = codegen.context

    val dataGenerator = codegen.objCDataGenerator!!

    fun FunctionGenerationContext.genSelector(selector: String): LLVMValueRef = genObjCSelector(selector)

    fun FunctionGenerationContext.genGetLinkedClass(name: String): LLVMValueRef {
        val classRef = dataGenerator.genClassRef(name)
        return load(classRef.llvm)
    }

    private val objcMsgSend = constPointer(
            context.llvm.externalFunction(LlvmFunction(
                    "objc_msgSend",
                    AttributedLlvmType(int8TypePtr),
                    listOf(AttributedLlvmType(int8TypePtr), AttributedLlvmType(int8TypePtr)),
                    isVararg = true,
                    origin = context.stdlibModule.llvmSymbolOrigin
            ))
    )

    val objcRelease = context.llvm.externalFunction(LlvmFunction(
            "objc_release",
            AttributedLlvmType(voidType),
            listOf(AttributedLlvmType(int8TypePtr)),
            origin = context.stdlibModule.llvmSymbolOrigin
    ))

    val objcAlloc = context.llvm.externalFunction(
            "objc_alloc",
            functionType(int8TypePtr, false, int8TypePtr),
            context.stdlibModule.llvmSymbolOrigin
    )

    val objcAutorelease = context.llvm.externalFunction(
            "llvm.objc.autorelease",
            functionType(int8TypePtr, false, int8TypePtr),
            context.stdlibModule.llvmSymbolOrigin
    ).also {
        setFunctionNoUnwind(it)
    }

    val objcAutoreleaseReturnValue = context.llvm.externalFunction(
            "llvm.objc.autoreleaseReturnValue",
            functionType(int8TypePtr, false, int8TypePtr),
            context.stdlibModule.llvmSymbolOrigin
    ).also {
        setFunctionNoUnwind(it)
    }

    // TODO: this doesn't support stret.
    fun msgSender(functionType: LLVMTypeRef): LLVMValueRef =
            objcMsgSend.bitcast(pointerType(functionType)).llvm
}

internal fun FunctionGenerationContext.genObjCSelector(selector: String): LLVMValueRef {
    val selectorRef = codegen.objCDataGenerator!!.genSelectorRef(selector)
    // TODO: clang emits it with `invariant.load` metadata.
    return load(selectorRef.llvm)
}