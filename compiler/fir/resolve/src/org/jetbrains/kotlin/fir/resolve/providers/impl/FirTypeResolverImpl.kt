/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.providers.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.ThreadSafeMutableState
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirOuterClassTypeParameterRef
import org.jetbrains.kotlin.fir.declarations.utils.isEnumClass
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.ConeUnexpectedTypeArgumentsError
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.calls.fullyExpandedClass
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeOuterClassArgumentsRequired
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedQualifierError
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnsupportedDynamicType
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeWrongNumberOfTypeArgumentsError
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.transformers.ScopeClassDeclaration
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.fir.types.impl.FirImplicitBuiltinTypeRef
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds

@ThreadSafeMutableState
class FirTypeResolverImpl(private val session: FirSession) : FirTypeResolver() {

    private val symbolProvider by lazy {
        session.symbolProvider
    }

    private data class ClassIdInSession(val session: FirSession, val id: ClassId)

    private val implicitBuiltinTypeSymbols = mutableMapOf<ClassIdInSession, FirClassLikeSymbol<*>>()

    // TODO: get rid of session used here, and may be also of the cache above (see KT-30275)
    private fun resolveBuiltInQualified(id: ClassId, session: FirSession): FirClassLikeSymbol<*> {
        val nameInSession = ClassIdInSession(session, id)
        return implicitBuiltinTypeSymbols.getOrPut(nameInSession) {
            symbolProvider.getClassLikeSymbolByClassId(id)!!
        }
    }

    private fun resolveToSymbol(
        typeRef: FirTypeRef,
        scope: FirScope,
    ): Pair<FirBasedSymbol<*>?, ConeSubstitutor?> {
        return when (typeRef) {
            is FirResolvedTypeRef -> {
                val resultSymbol = typeRef.coneTypeSafe<ConeLookupTagBasedType>()?.lookupTag?.let(symbolProvider::getSymbolByLookupTag)
                resultSymbol to null
            }

            is FirUserTypeRef -> {
                val qualifierResolver = session.qualifierResolver
                var resolvedSymbol: FirBasedSymbol<*>? = null
                var substitutor: ConeSubstitutor? = null
                scope.processClassifiersByNameWithSubstitution(typeRef.qualifier.first().name) { symbol, substitutorFromScope ->
                    if (resolvedSymbol != null) return@processClassifiersByNameWithSubstitution
                    resolvedSymbol = when (symbol) {
                        is FirClassLikeSymbol<*> -> {
                            if (typeRef.qualifier.size == 1) {
                                symbol
                            } else {
                                qualifierResolver.resolveSymbolWithPrefix(typeRef.qualifier, symbol.classId)
                                    ?: qualifierResolver.resolveEnumEntrySymbol(typeRef.qualifier, symbol.classId)
                            }
                        }
                        is FirTypeParameterSymbol -> {
                            assert(typeRef.qualifier.size == 1)
                            symbol
                        }
                        else -> error("!")
                    }
                    substitutor = substitutorFromScope
                }

                // TODO: Imports
                val resultSymbol: FirBasedSymbol<*>? = resolvedSymbol ?: qualifierResolver.resolveSymbol(typeRef.qualifier)
                resultSymbol to substitutor
            }

            is FirImplicitBuiltinTypeRef -> {
                resolveBuiltInQualified(typeRef.id, session) to null
            }

            else -> null to null
        }
    }

    @OptIn(SymbolInternals::class)
    private fun FirQualifierResolver.resolveEnumEntrySymbol(
        qualifier: List<FirQualifierPart>,
        classId: ClassId
    ): FirVariableSymbol<FirEnumEntry>? {
        // Assuming the current qualifier refers to an enum entry, we drop the last part so we get a reference to the enum class.
        val enumClassSymbol = resolveSymbolWithPrefix(qualifier.dropLast(1), classId) ?: return null
        val enumClassFir = enumClassSymbol.fir as? FirRegularClass ?: return null
        if (!enumClassFir.isEnumClass) return null
        val enumEntryMatchingLastQualifier = enumClassFir.declarations
            .firstOrNull { it is FirEnumEntry && it.name == qualifier.last().name } as? FirEnumEntry
        return enumEntryMatchingLastQualifier?.symbol
    }

    @OptIn(SymbolInternals::class)
    private fun resolveUserType(
        typeRef: FirUserTypeRef,
        symbol: FirBasedSymbol<*>?,
        substitutor: ConeSubstitutor?,
        areBareTypesAllowed: Boolean,
        topDeclaration: FirDeclaration?,
        isOperandOfIsOperator: Boolean
    ): ConeKotlinType {
        if (symbol == null || symbol !is FirClassifierSymbol<*>) {
            val diagnostic = if (symbol?.fir is FirEnumEntry) {
                if (isOperandOfIsOperator) {
                    ConeSimpleDiagnostic("'is' operator can not be applied to an enum entry.", DiagnosticKind.IsEnumEntry)
                } else {
                    ConeSimpleDiagnostic("An enum entry should not be used as a type.", DiagnosticKind.EnumEntryAsType)
                }
            } else {
                ConeUnresolvedQualifierError(typeRef.render())
            }
            return ConeKotlinErrorType(diagnostic)
        }
        if (symbol is FirTypeParameterSymbol) {
            for (part in typeRef.qualifier) {
                if (part.typeArgumentList.typeArguments.isNotEmpty()) {
                    return ConeClassErrorType(
                        ConeUnexpectedTypeArgumentsError("Type arguments not allowed", part.typeArgumentList.source)
                    )
                }
            }
        }

        val allTypeArguments = mutableListOf<ConeTypeProjection>()
        var typeArgumentsCount = 0

        val qualifier = typeRef.qualifier
        for (qualifierIndex in qualifier.size - 1 downTo 0) {
            val qualifierTypeArguments = qualifier[qualifierIndex].typeArgumentList.typeArguments

            for (qualifierTypeArgument in qualifierTypeArguments) {
                allTypeArguments.add(qualifierTypeArgument.toConeTypeProjection())
                typeArgumentsCount++
            }
        }

        if (symbol is FirClassLikeSymbol<*>) {
            val isPossibleBareType = areBareTypesAllowed && allTypeArguments.isEmpty()
            if (!isPossibleBareType) {
                val actualSubstitutor = substitutor ?: ConeSubstitutor.Empty

                val originalTypeParameters = collectTypeParameters(symbol)

                val (typeParametersAlignedToQualifierParts, outerDeclarations) = getClassesAlignedToQualifierParts(
                    symbol,
                    qualifier,
                    session
                )

                val actualTypeParametersCount =
                    when (symbol) {
                        is FirTypeAliasSymbol ->
                            outerDeclarations.sumOf { it?.let { d -> getActualTypeParametersCount(d) } ?: 0 }
                        else -> (symbol as FirClassSymbol<*>).typeParameterSymbols.size
                    }

                for ((typeParameterIndex, typeParameter) in originalTypeParameters.withIndex()) {
                    val (parameterClass, qualifierPartIndex) = typeParametersAlignedToQualifierParts[typeParameter.symbol] ?: continue

                    if (typeParameterIndex < typeArgumentsCount) {
                        // Check if type argument matches type parameter in respective qualifier part
                        val qualifierPartArgumentsCount = qualifier[qualifierPartIndex].typeArgumentList.typeArguments.size
                        createDiagnosticsIfExists(
                            parameterClass,
                            qualifierPartIndex,
                            symbol,
                            typeRef,
                            qualifierPartArgumentsCount
                        )?.let { return it }
                        continue
                    }

                    if (typeParameter !is FirOuterClassTypeParameterRef ||
                        isValidTypeParameterFromOuterDeclaration(typeParameter.symbol, topDeclaration, session)
                    ) {
                        val type = ConeTypeParameterTypeImpl(ConeTypeParameterLookupTag(typeParameter.symbol), isNullable = false)
                        val substituted = actualSubstitutor.substituteOrNull(type)
                        if (substituted == null) {
                            createDiagnosticsIfExists(
                                parameterClass,
                                qualifierPartIndex,
                                symbol,
                                typeRef,
                                qualifierPartArgumentsCount = null
                            )?.let { return it }
                        } else {
                            allTypeArguments.add(substituted)
                        }
                    } else {
                        return ConeClassErrorType(ConeOuterClassArgumentsRequired(parameterClass.symbol))
                    }
                }

                // Check rest type arguments
                if (typeArgumentsCount > actualTypeParametersCount) {
                    for (index in qualifier.indices) {
                        if (qualifier[index].typeArgumentList.typeArguments.isNotEmpty()) {
                            val parameterClass = outerDeclarations.elementAtOrNull(index)
                            createDiagnosticsIfExists(
                                parameterClass,
                                index,
                                symbol,
                                typeRef,
                                qualifierPartArgumentsCount = null
                            )?.let { return it }
                        }
                    }
                }
            }
        }

        return symbol.constructType(allTypeArguments.toTypedArray(), typeRef.isMarkedNullable, typeRef.annotations.computeTypeAttributes())
            .also {
                val lookupTag = it.lookupTag
                if (lookupTag is ConeClassLikeLookupTagImpl && symbol is FirClassLikeSymbol<*>) {
                    lookupTag.bindSymbolToLookupTag(session, symbol)
                }
            }
    }

    private fun collectTypeParameters(symbol: FirClassLikeSymbol<*>): List<FirTypeParameterRef> {
        if (symbol is FirClassSymbol<*>) {
            return symbol.fir.typeParameters
        }

        require(symbol is FirTypeAliasSymbol)

        val typeAliasFir = symbol.fir
        val typeAliasTypeParameters = typeAliasFir.typeParameters.toMutableList()
        val fullyExpandedClass = typeAliasFir.fullyExpandedClass(session)
        val newTypeParameters = if (fullyExpandedClass != null) { // TODO: Should not be null, move to resolver?
            val expandedTypeRef = typeAliasFir.expandedTypeRef

            fun checkTypeArguments(typeArgument: ConeTypeProjection) {
                when (typeArgument) {
                    is ConeTypeParameterType -> typeAliasTypeParameters.removeIf { it.symbol == typeArgument.lookupTag.symbol }
                    is ConeClassLikeType -> {
                        for (subTypeArgument in typeArgument.typeArguments) {
                            checkTypeArguments(subTypeArgument)
                        }
                    }
                    is ConeKotlinTypeProjection -> checkTypeArguments(typeArgument.type)
                    else -> {
                    }
                }
            }

            checkTypeArguments(expandedTypeRef.coneType)
            fullyExpandedClass.typeParameters.toMutableList()
        } else {
            mutableListOf()
        }

        for (typeParameter in typeAliasTypeParameters) {
            newTypeParameters.add(typeParameter)
        }
        return newTypeParameters
    }

    @OptIn(SymbolInternals::class)
    private fun getClassesAlignedToQualifierParts(
        symbol: FirClassLikeSymbol<*>,
        qualifier: List<FirQualifierPart>,
        session: FirSession
    ): ParametersMapAndOuterClasses {
        var currentClassLikeDeclaration: FirClassLikeDeclaration? = null
        val outerDeclarations = mutableListOf<FirClassLikeDeclaration?>()

        // Try to get at least qualifier.size classes that match qualifier parts
        var qualifierPartIndex = 0
        while (qualifierPartIndex < qualifier.size || currentClassLikeDeclaration != null) {
            if (qualifierPartIndex == 0) {
                currentClassLikeDeclaration = symbol.fir
            } else {
                if (currentClassLikeDeclaration != null) {
                    currentClassLikeDeclaration = currentClassLikeDeclaration.getContainingDeclaration(session)
                }
            }

            outerDeclarations.add(currentClassLikeDeclaration)
            qualifierPartIndex++
        }

        val outerArgumentsCount = outerDeclarations.size - qualifier.size
        val reversedOuterClasses = outerDeclarations.asReversed()
        val result = mutableMapOf<FirTypeParameterSymbol, ClassWithQualifierPartIndex>()

        for (index in reversedOuterClasses.indices) {
            currentClassLikeDeclaration = reversedOuterClasses[index]
            val typeParameters = when (currentClassLikeDeclaration) {
                is FirTypeAlias -> currentClassLikeDeclaration.typeParameters
                else -> (currentClassLikeDeclaration as? FirClass)?.typeParameters
            }
            if (currentClassLikeDeclaration != null && typeParameters != null) {
                for (typeParameter in typeParameters) {
                    val typeParameterSymbol = typeParameter.symbol
                    if (!result.containsKey(typeParameterSymbol)) {
                        result[typeParameterSymbol] = ClassWithQualifierPartIndex(currentClassLikeDeclaration, index - outerArgumentsCount)
                    }
                }
            }
        }

        return ParametersMapAndOuterClasses(result, reversedOuterClasses.drop(outerArgumentsCount))
    }

    private data class ParametersMapAndOuterClasses(
        val parameters: Map<FirTypeParameterSymbol, ClassWithQualifierPartIndex>,
        val outerClasses: List<FirClassLikeDeclaration?>
    )

    private data class ClassWithQualifierPartIndex(
        val klass: FirClassLikeDeclaration,
        val index: Int
    )

    @OptIn(SymbolInternals::class)
    private fun createDiagnosticsIfExists(
        parameterClass: FirClassLikeDeclaration?,
        qualifierPartIndex: Int,
        symbol: FirClassLikeSymbol<*>,
        userTypeRef: FirUserTypeRef,
        qualifierPartArgumentsCount: Int?
    ): ConeClassErrorType? {
        // TODO: It should be TYPE_ARGUMENTS_NOT_ALLOWED diagnostics when parameterClass is null
        val actualTypeParametersCount = getActualTypeParametersCount(parameterClass ?: symbol.fir)

        if (qualifierPartArgumentsCount == null || actualTypeParametersCount != qualifierPartArgumentsCount) {
            val source = getTypeArgumentsOrNameSource(userTypeRef, qualifierPartIndex)
            if (source != null) {
                return ConeClassErrorType(
                    ConeWrongNumberOfTypeArgumentsError(
                        actualTypeParametersCount,
                        parameterClass?.symbol ?: symbol,
                        source
                    )
                )
            }
        }

        return null
    }

    private fun getActualTypeParametersCount(element: FirClassLikeDeclaration): Int {
        return (element as FirTypeParameterRefsOwner).typeParameters
            .count { it !is FirOuterClassTypeParameterRef }
    }

    private fun getTypeArgumentsOrNameSource(typeRef: FirUserTypeRef, qualifierIndex: Int?): FirSourceElement? {
        val qualifierPart = if (qualifierIndex != null) typeRef.qualifier.elementAtOrNull(qualifierIndex) else null
        val typeArgumentsList = qualifierPart?.typeArgumentList
        return if (typeArgumentsList == null || typeArgumentsList.typeArguments.isEmpty()) {
            qualifierPart?.source ?: typeRef.source
        } else {
            typeArgumentsList.source
        }
    }

    private fun createFunctionalType(typeRef: FirFunctionTypeRef): ConeClassLikeType {
        val parameters =
            listOfNotNull(typeRef.receiverTypeRef?.coneType) +
                    typeRef.valueParameters.map { it.returnTypeRef.coneType } +
                    listOf(typeRef.returnTypeRef.coneType)
        val classId = if (typeRef.isSuspend) {
            StandardClassIds.SuspendFunctionN(typeRef.parametersCount)
        } else {
            StandardClassIds.FunctionN(typeRef.parametersCount)
        }
        val attributes = typeRef.annotations.computeTypeAttributes()
        val symbol = resolveBuiltInQualified(classId, session)
        return ConeClassLikeTypeImpl(
            symbol.toLookupTag().also {
                if (it is ConeClassLikeLookupTagImpl) {
                    it.bindSymbolToLookupTag(session, symbol)
                }
            },
            parameters.toTypedArray(),
            typeRef.isMarkedNullable,
            attributes
        )
    }

    override fun resolveType(
        typeRef: FirTypeRef,
        scopeClassDeclaration: ScopeClassDeclaration,
        areBareTypesAllowed: Boolean,
        isOperandOfIsOperator: Boolean
    ): ConeKotlinType {
        return when (typeRef) {
            is FirResolvedTypeRef -> typeRef.type
            is FirUserTypeRef -> {
                val (symbol, substitutor) = resolveToSymbol(typeRef, scopeClassDeclaration.scope)
                resolveUserType(
                    typeRef,
                    symbol,
                    substitutor,
                    areBareTypesAllowed,
                    scopeClassDeclaration.topDeclaration,
                    isOperandOfIsOperator
                )
            }
            is FirFunctionTypeRef -> createFunctionalType(typeRef)
            is FirDynamicTypeRef -> ConeKotlinErrorType(ConeUnsupportedDynamicType())
            else -> error(typeRef.render())
        }
    }
}
