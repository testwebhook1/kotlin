/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended

import org.jetbrains.kotlin.KtFakeSourceElement
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.overrideModifier
import org.jetbrains.kotlin.diagnostics.visibilityModifier
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.context.findClosest
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.isLocalMember
import org.jetbrains.kotlin.fir.analysis.checkers.findClosestClassOrObject
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.analysis.checkers.overriddenFunctions
import org.jetbrains.kotlin.fir.analysis.checkers.syntax.FirDeclarationSyntaxChecker
import org.jetbrains.kotlin.fir.analysis.checkers.toVisibilityOrNull
import org.jetbrains.kotlin.fir.analysis.diagnostics.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isEnumClass
import org.jetbrains.kotlin.fir.declarations.utils.isOverride
import org.jetbrains.kotlin.fir.declarations.utils.isSealed
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.psi.KtDeclaration

object RedundantVisibilityModifierSyntaxChecker : FirDeclarationSyntaxChecker<FirDeclaration, KtDeclaration>() {

    override fun checkLightTree(
        element: FirDeclaration,
        source: KtSourceElement,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        if (element is FirConstructor && source.kind is KtFakeSourceElementKind) return
        if (source is KtFakeSourceElement) return
        if (
            element !is FirMemberDeclaration
            && !(element is FirPropertyAccessor && element.visibility == context.containingPropertyVisibility)
        ) return

        val visibilityModifier = source.treeStructure.visibilityModifier(source.lighterASTNode)
        val explicitVisibility = (visibilityModifier?.tokenType as? KtModifierKeywordToken)?.toVisibilityOrNull()
        val implicitVisibility = element.implicitVisibility(context)
        val containingMemberDeclaration = context.findClosest<FirMemberDeclaration>()
        require(containingMemberDeclaration is FirDeclaration?)

        val redundantVisibility = when {
            explicitVisibility == implicitVisibility -> implicitVisibility
            explicitVisibility == Visibilities.Internal && containingMemberDeclaration?.isLocalMember == true -> Visibilities.Internal
            else -> return
        }

        if (
            redundantVisibility == Visibilities.Public
            && element is FirProperty
            && source.treeStructure.overrideModifier(source.lighterASTNode) != null
            && element.isVar
            && element.setter?.visibility == Visibilities.Public
        ) return

        reporter.reportOn(source, FirErrors.REDUNDANT_VISIBILITY_MODIFIER, context)
    }

    private fun FirDeclaration.implicitVisibility(context: CheckerContext): Visibility {
        return when {
            this is FirPropertyAccessor && isSetter && status.isOverride -> this.visibility

            this is FirPropertyAccessor -> {
                context.findClosest<FirProperty>()?.visibility ?: Visibilities.DEFAULT_VISIBILITY
            }

            this is FirConstructor -> {
                val classSymbol = this.getContainingClassSymbol(context.session)
                if (
                    classSymbol is FirRegularClassSymbol
                    && (classSymbol.isEnumClass || classSymbol.isSealed)
                ) {
                    Visibilities.Private
                } else {
                    Visibilities.DEFAULT_VISIBILITY
                }
            }

            this is FirSimpleFunction
                    && context.containingDeclarations.last() is FirClass
                    && this.isOverride -> findFunctionVisibility(this, context)

            else -> Visibilities.DEFAULT_VISIBILITY
        }
    }

    private fun findFunctionVisibility(function: FirSimpleFunction, context: CheckerContext): Visibility {
        val currentClassSymbol = context.findClosestClassOrObject()?.symbol ?: return Visibilities.Unknown
        val overriddenFunctions = function.overriddenFunctions(currentClassSymbol, context)
        var visibility: Visibility = Visibilities.Private
        for (func in overriddenFunctions) {
            val currentVisibility = func.visibility
            val compareResult = Visibilities.compare(currentVisibility, visibility)
            if (compareResult != null && compareResult > 0) {
                visibility = currentVisibility
            }
        }

        return visibility
    }

    private val CheckerContext.containingPropertyVisibility
        get() = (this.containingDeclarations.last() as? FirProperty)?.visibility
}
