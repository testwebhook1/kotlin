/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols.markers

import org.jetbrains.kotlin.analysis.api.symbols.KtEnumEntrySymbol
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.types.ConstantValueKind

/**
 * Annotation values are expected to be compile-time constants. According to the
 * [spec](https://kotlinlang.org/spec/annotations.html#annotation-values),
 * allowed kinds are:
 *   * integer types,
 *   * string type,
 *   * enum types,
 *   * other annotation types, and
 *   * array of aforementioned types
 *
 *  [KtSimpleConstantValue] covers first two kinds;
 *  [KtEnumEntryValue] corresponds to enum types;
 *  [KtAnnotationConstantValue] represents annotation types (with annotation fq name and arguments); and
 *  [KtArrayConstantValue] abstracts an array of [KtConstantValue]s.
 */
public sealed class KtConstantValue(
    public open val kotlinOrigin: KtElement? = null
)
public object KtUnsupportedConstantValue : KtConstantValue()

public class KtArrayConstantValue(
    public val values: Collection<KtConstantValue>,
    override val kotlinOrigin: KtElement?,
) : KtConstantValue()

public class KtAnnotationConstantValue(
    public val fqName: String?,
    public val arguments: List<KtNamedConstantValue>,
    override val kotlinOrigin: KtCallElement?,
) : KtConstantValue()

public class KtEnumEntryValue(
    public val enumEntrySymbol: KtEnumEntrySymbol,
    override val kotlinOrigin: KtElement?,
) : KtConstantValue()

public class KtSimpleConstantValue<T>(
    public val constantValueKind: ConstantValueKind<T>,
    public val value: T,
    override val kotlinOrigin: KtElement?,
) : KtConstantValue() {
    public fun toConst(): Any? {
        return (value as? Long)?.let {
            when (constantValueKind) {
                ConstantValueKind.Byte -> it.toByte()
                ConstantValueKind.Short -> it.toShort()
                ConstantValueKind.Int -> it.toInt()
                ConstantValueKind.Float -> it.toFloat()
                ConstantValueKind.Double -> it.toDouble()

                ConstantValueKind.UnsignedByte -> it.toUByte()
                ConstantValueKind.UnsignedShort -> it.toUShort()
                ConstantValueKind.UnsignedInt -> it.toUInt()
                ConstantValueKind.UnsignedLong -> it.toULong()
                else -> it
            }
        } ?: value
    }

    override fun toString(): String {
        return "KtSimpleConstantValue(constantValueKind=${constantValueKind}, value=${value})"
    }
}
