/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols.annotations

import org.jetbrains.kotlin.analysis.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirAnnotatedDeclaration
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.ResolveType
import org.jetbrains.kotlin.analysis.api.fir.utils.FirRefWithValidityCheck
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.resolved
import org.jetbrains.kotlin.fir.declarations.utils.primaryConstructor
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.name.ClassId

internal fun FirAnnotation.getClassId(session: FirSession): ClassId? =
    coneClassLikeType?.fullyExpandedType(session)?.classId

internal fun FirRefWithValidityCheck<FirAnnotatedDeclaration>.toAnnotationsList(
    builder: KtSymbolByFirBuilder
) = withFir { fir ->
    fir.annotations.map { KtFirAnnotationCall(this, it, builder) }
}

internal fun FirRefWithValidityCheck<FirAnnotatedDeclaration>.containsAnnotation(classId: ClassId): Boolean =
    withFirByType(ResolveType.AnnotationType) { fir ->
        fir.annotations.any { it.getClassId(fir.moduleData.session) == classId }
    }

internal fun FirRefWithValidityCheck<FirAnnotatedDeclaration>.getAnnotationClassIds(): Collection<ClassId> =
    withFirByType(ResolveType.AnnotationType) { fir ->
        fir.annotations.mapNotNull { it.getClassId(fir.moduleData.session) }
    }

internal fun mapAnnotationParameters(annotation: FirAnnotation, session: FirSession): Map<String, FirExpression> {
    if (annotation.resolved) return annotation.argumentMapping.mapping.mapKeys { (name, _) -> name.identifier }
    if (annotation !is FirAnnotationCall) return emptyMap()
    val annotationCone = annotation.annotationTypeRef.coneType as? ConeClassLikeType ?: return emptyMap()

    val annotationPrimaryCtor = (annotationCone.lookupTag.toSymbol(session)?.fir as? FirRegularClass)?.primaryConstructor
    val annotationCtorParameterNames = annotationPrimaryCtor?.valueParameters?.map { it.name }

    val resultSet = mutableMapOf<String, FirExpression>()

    val namesSequence = annotationCtorParameterNames?.asSequence()?.iterator()

    for (argument in annotation.argumentList.arguments.filterIsInstance<FirNamedArgumentExpression>()) {
        resultSet[argument.name.asString()] = argument.expression
    }

    for (argument in annotation.argumentList.arguments) {
        if (argument is FirNamedArgumentExpression) continue

        while (namesSequence != null && namesSequence.hasNext()) {
            val name = namesSequence.next().asString()
            if (!resultSet.contains(name)) {
                resultSet[name] = argument
                break
            }
        }
    }

    return resultSet
}
