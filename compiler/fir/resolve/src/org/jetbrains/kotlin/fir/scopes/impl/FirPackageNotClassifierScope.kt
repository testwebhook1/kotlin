/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

class FirPackageNotClassifierScope(
    val packageMemberScope: FirPackageMemberScope,
    val explicitSimpleImportingScope: FirExplicitSimpleImportingScope
) : FirScope() {
    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        packageMemberScope.processPropertiesByName(name, processor)
    }

    override fun processFunctionsByName(name: Name, processor: (FirNamedFunctionSymbol) -> Unit) {
        if (name.asString().isEmpty()) return

        if (packageMemberScope.classifierCache[name] == null) {
            val symbol = packageMemberScope.symbolProvider.getClassLikeSymbolByFqName(ClassId(packageMemberScope.fqName, name))
            if (symbol is FirClassLikeSymbol) {
                return
            }
        } else {
            return
        }

        if ((explicitSimpleImportingScope.simpleImports[name]?.size ?: 0) > 0) {
            explicitSimpleImportingScope.processFunctionsByName(name, processor)
            return
        }

        packageMemberScope.processFunctionsByName(name, processor)
    }

    override val scopeOwnerLookupNames: List<String> = packageMemberScope.scopeOwnerLookupNames
}