/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols.annotations

import org.jetbrains.kotlin.analysis.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirAnnotatedDeclaration
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.coneClassLikeType
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.ResolveType
import org.jetbrains.kotlin.analysis.api.fir.utils.FirRefWithValidityCheck
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
