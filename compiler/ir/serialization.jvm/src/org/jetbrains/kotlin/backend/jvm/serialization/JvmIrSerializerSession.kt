/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.serialization

import org.jetbrains.kotlin.backend.common.serialization.CompatibilityMode
import org.jetbrains.kotlin.backend.common.serialization.DeclarationTable
import org.jetbrains.kotlin.backend.common.serialization.IrFileSerializer
import org.jetbrains.kotlin.backend.jvm.serialization.proto.JvmIr
import org.jetbrains.kotlin.config.JvmSerializeIrMode
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.IrMessageLogger
import org.jetbrains.kotlin.protobuf.ByteString

class JvmIrSerializerSession(
    messageLogger: IrMessageLogger,
    private val declarationTable: DeclarationTable,
    expectDescriptorToSymbol: MutableMap<DeclarationDescriptor, IrSymbol>,
    mode: JvmSerializeIrMode,
    skipExpects: Boolean = false,
) : IrFileSerializer(
    messageLogger, declarationTable, expectDescriptorToSymbol, CompatibilityMode.CURRENT,
    bodiesOnlyForInlines = mode == JvmSerializeIrMode.INLINE,
    skipExpects
) {
    init {
        assert(mode != JvmSerializeIrMode.NONE)
    }

    // Usage protocol: construct an instance, call only one of `serializeIrFile()` and `serializeTopLevelClass()` only once.

    fun serializeJvmIrFile(irFile: IrFile): JvmIr.JvmIrFile {
        val proto = JvmIr.JvmIrFile.newBuilder()

        declarationTable.inFile(irFile) {
            irFile.declarations.filter { it !is IrClass }.forEach { declaration ->
                proto.addDeclaration(serializeDeclaration(declaration))
            }
            proto.addAllAnnotation(serializeAnnotations(irFile.annotations))
        }

        proto.auxTables = serializeAuxTables()

        return proto.build()
    }

    fun serializeTopLevelClass(irClass: IrClass): JvmIr.JvmIrClass {
        val proto = JvmIr.JvmIrClass.newBuilder()
        declarationTable.inFile(irClass.parent as IrFile) {
            proto.irClass = serializeIrClass(irClass)
        }
        proto.auxTables = serializeAuxTables()
        return proto.build()
    }

    private fun serializeAuxTables(): JvmIr.AuxTables {
        val proto = JvmIr.AuxTables.newBuilder()
        protoTypeArray.forEach { proto.addType(it.toByteString()) }
        protoIdSignatureArray.forEach { proto.addSignature(it.toByteString()) }
        protoStringArray.forEach { proto.addString(ByteString.copyFromUtf8(it)) }
        protoBodyArray.forEach { proto.addBody(ByteString.copyFrom(it.toByteArray())) }
        protoDebugInfoArray.forEach { proto.addDebugInfo(ByteString.copyFromUtf8(it)) }
        return proto.build()
    }
}