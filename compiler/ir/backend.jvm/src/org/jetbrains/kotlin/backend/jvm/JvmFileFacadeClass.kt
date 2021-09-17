/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrClassSymbolImpl
import org.jetbrains.kotlin.ir.util.DeclarationStubGenerator
import org.jetbrains.kotlin.ir.util.DeserializableClass
import org.jetbrains.kotlin.name.Name

class JvmFileFacadeClass(
    origin: IrDeclarationOrigin,
    name: Name,
    source: SourceElement,
    private val stubGenerator: DeclarationStubGenerator
) : IrClassImpl(
    UNDEFINED_OFFSET, UNDEFINED_OFFSET, origin,
    IrClassSymbolImpl(), name, ClassKind.CLASS, DescriptorVisibilities.PUBLIC, Modality.FINAL,
    source = source
), DeserializableClass {

    private var irLoadingStarted: Boolean = false

    /*  loadIr is always invoked under lock, so no race is possible.
       However when performing after-deserialization setup, a recursive attempt to load may occur
       when non-serialized declarations are touched. We need to prevent those, hence the need for the irLoadingStarted variable.
       Upon successful load, irLoaded will be set by the deserializer.
    */
    override fun loadIr() {
        if (irLoadingStarted) return
        irLoadingStarted = true
        stubGenerator.extensions.deserializeFacadeClass(this, stubGenerator, parent, allowErrorNodes = false)
    }
}