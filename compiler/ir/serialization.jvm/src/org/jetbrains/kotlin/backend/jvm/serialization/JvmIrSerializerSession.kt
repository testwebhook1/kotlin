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
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.IrMessageLogger
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.protobuf.ByteString

class JvmIrSerializerSession(
    messageLogger: IrMessageLogger,
    private val declarationTable: DeclarationTable,
    expectDescriptorToSymbol: MutableMap<DeclarationDescriptor, IrSymbol>,
    private val mode: JvmSerializeIrMode,
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

    fun serializeJvmIrFile(irFile: IrFile): JvmIr.JvmIrFile? {
        var anySaved: Boolean = false
        val proto = JvmIr.JvmIrFile.newBuilder()

        declarationTable.inFile(irFile) {
            irFile.declarations.filter { it !is IrClass && it.needsSerialization() }.forEach { declaration ->
                proto.addDeclaration(serializeDeclaration(declaration))
                anySaved = true
            }
            proto.addAllAnnotation(serializeAnnotations(irFile.annotations))
        }
        if (!anySaved) return null

        proto.auxTables = serializeAuxTables()

        return proto.build()
    }

    fun serializeTopLevelClass(irClass: IrClass): JvmIr.JvmIrClass? {
        if (!irClass.needsSerialization()) return null

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

    // In INLINE serialization mode, avoid serializing useless declarations
    override fun memberNeedsSerialization(member: IrDeclaration): Boolean {
        if (!member.needsSerialization()) return false
        return super.memberNeedsSerialization(member)
    }

    private fun IrDeclaration.needsSerialization(): Boolean =
        when (mode) {
            JvmSerializeIrMode.NONE -> error("method should not even be called with serialization mode NONE")
            JvmSerializeIrMode.ALL -> true
            JvmSerializeIrMode.INLINE -> isInlineOrInsideInlineOrHasInlineSubdeclarations()
        }

    private fun IrDeclaration.isInlineOrInsideInlineOrHasInlineSubdeclarations(): Boolean =
        accept(inlineCheckVisitor, Unit)

    private val inlineCheckVisitor = InlineCheckVisitor()

    private class InlineCheckVisitor : IrElementVisitor<Boolean, Unit> {
        override fun visitElement(element: IrElement, data: Unit): Boolean = error("Should never be called")

        override fun visitDeclaration(declaration: IrDeclarationBase, data: Unit): Boolean {
            val parent = declaration.parent
            return parent !is IrClass || parent.visibility == DescriptorVisibilities.LOCAL
        }

        override fun visitSimpleFunction(declaration: IrSimpleFunction, data: Unit): Boolean {
            return declaration.visibility == DescriptorVisibilities.LOCAL ||
                    declaration.isInline && declaration.visibility.visibleFromOtherFiles ||
                    visitDeclaration(declaration, data)
        }

        override fun visitProperty(declaration: IrProperty, data: Unit): Boolean {
            return declaration.visibility == DescriptorVisibilities.LOCAL ||
                    (declaration.getter?.isInline == true ||
                            declaration.setter?.isInline == true) && declaration.visibility.visibleFromOtherFiles ||
                    visitDeclaration(declaration, data)
        }

        override fun visitClass(declaration: IrClass, data: Unit): Boolean {
            return declaration.visibility == DescriptorVisibilities.LOCAL ||
                    (declaration.visibility.visibleFromOtherFiles &&
                            declaration.declarations.any { it.accept(this, data) }) ||
                    visitDeclaration(declaration, data)
        }

        private val DescriptorVisibility.visibleFromOtherFiles
            get() = isPublicAPI ||
                    this == DescriptorVisibilities.INTERNAL
    }
}

