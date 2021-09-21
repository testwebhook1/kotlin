/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import com.intellij.lang.LighterASTNode
import com.intellij.lang.TreeBackedLighterAST
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.tree.IElementType
import com.intellij.util.diff.FlyweightCapableTreeStructure

sealed class FirSourceElementKind

object FirRealSourceElementKind : FirSourceElementKind()

sealed class FirFakeSourceElementKind : FirSourceElementKind() {
    // for some fir expression implicit return typeRef is generated
    // some of them are: break, continue, return, throw, string concat,
    // destruction parameters, function literals, explicitly boolean expressions
    object ImplicitTypeRef : FirFakeSourceElementKind()

    // for each class special class self type ref is created
    // and have a fake source referencing it
    object ClassSelfTypeRef : FirFakeSourceElementKind()

    // FirErrorTypeRef may be built using unresolved firExpression
    // and have a fake source referencing it
    object ErrorTypeRef : FirFakeSourceElementKind()

    // for properties without accessors default getter & setter are generated
    // they have a fake source which refers to property
    object DefaultAccessor : FirFakeSourceElementKind()

    // for delegated properties, getter & setter calls to the delegate
    // they have a fake source which refers to the call that creates the delegate
    object DelegatedPropertyAccessor : FirFakeSourceElementKind()

    // for kt classes without implicit primary constructor one is generated
    // with a fake source which refers to containing class
    object ImplicitConstructor : FirFakeSourceElementKind()

    // for constructors which do not have delegated constructor call the fake one is generated
    // with a fake sources which refers to the original constructor
    object DelegatingConstructorCall : FirFakeSourceElementKind()

    // for enum entry with bodies the initializer in a form of anonymous object is generated
    // with a fake sources which refers to the enum entry
    object EnumInitializer : FirFakeSourceElementKind()

    // for lambdas with implicit return the return statement is generated which is labeled
    // with a fake sources which refers to the target expression
    object GeneratedLambdaLabel : FirFakeSourceElementKind()

    // for lambdas & functions with expression bodies the return statement is added
    // with a fake sources which refers to the return target
    object ImplicitReturn : FirFakeSourceElementKind()

    // return expression in procedures -> return Unit
    // with a fake sources which refers to the return statement
    object ImplicitUnit : FirFakeSourceElementKind()

    // delegates are wrapped into FirWrappedDelegateExpression
    // with a fake sources which refers to delegated expression
    object WrappedDelegate : FirFakeSourceElementKind()

    //  `for (i in list) { println(i) }` is converted to
    //  ```
    //  val <iterator>: = list.iterator()
    //  while(<iterator>.hasNext()) {
    //    val i = <iterator>.next()
    //    println(i)
    //  }
    //  ```
    //  where the generated WHILE loop has source element of initial FOR loop,
    //  other generated elements are marked as fake ones
    object DesugaredForLoop : FirFakeSourceElementKind()

    object ImplicitInvokeCall : FirFakeSourceElementKind()

    // Consider an atomic qualified access like `i`. In the FIR tree, both the FirQualifiedAccessExpression and its calleeReference uses
    // `i` as the source. Hence, this fake kind is set on the `calleeReference` to make sure no PSI element is shared by multiple FIR
    // elements. This also applies to `this` and `super` references.
    object ReferenceInAtomicQualifiedAccess : FirFakeSourceElementKind()

    // for enum classes we have valueOf & values functions generated
    // with a fake sources which refers to this the enum class
    object EnumGeneratedDeclaration : FirFakeSourceElementKind()

    // when (x) { "abc" -> 42 } --> when(val $subj = x) { $subj == "abc" -> 42 }
    // where $subj == "42" has fake psi source which refers to "42" as inner expression
    // and $subj fake source refers to "42" as KtWhenCondition
    object WhenCondition : FirFakeSourceElementKind()


    // for primary constructor parameter the corresponding class property is generated
    // with a fake sources which refers to this the corresponding parameter
    object PropertyFromParameter : FirFakeSourceElementKind()

    // if (true) 1 --> if(true) { 1 }
    // with a fake sources for the block which refers to the wrapped expression
    object SingleExpressionBlock : FirFakeSourceElementKind()

    // x++ -> x = x.inc()
    // x = x++ -> x = { val <unary> = x; x = <unary>.inc(); <unary> }
    object DesugaredIncrementOrDecrement : FirFakeSourceElementKind()

    // x !in list --> !(x in list) where ! and !(x in list) will have a fake source
    object DesugaredInvertedContains : FirFakeSourceElementKind()

    // for data classes fir generates componentN() & copy() functions
    // for componentN() functions the source will refer to the corresponding param and will be marked as a fake one
    // for copy() functions the source will refer class to the param and will be marked as a fake one
    object DataClassGeneratedMembers : FirFakeSourceElementKind()

    // (vararg x: Int) --> (x: Array<out Int>) where array type ref has a fake source kind
    object ArrayTypeFromVarargParameter : FirFakeSourceElementKind()

    // val (a,b) = x --> val a = x.component1(); val b = x.component2()
    // where componentN calls will have the fake source elements refer to the corresponding KtDestructuringDeclarationEntry
    object DesugaredComponentFunctionCall : FirFakeSourceElementKind()

    // when smart casts applied to the expression, its wrapped into FirExpressionWithSmartcast
    // which type reference will have a fake source refer to a original source element of it
    object SmartCastedTypeRef : FirFakeSourceElementKind()

    // for safe call expressions like a?.foo() the FirSafeCallExpression is generated
    // and it have a fake source
    object DesugaredSafeCallExpression : FirFakeSourceElementKind()

    // a += 2 --> a = a + 2
    // where a + 2 will have a fake source
    object DesugaredCompoundAssignment : FirFakeSourceElementKind()

    // `a > b` will be wrapped in FirComparisonExpression
    // with real source which points to initial `a > b` expression
    // and inner FirFunctionCall will refer to a fake source
    object GeneratedComparisonExpression : FirFakeSourceElementKind()

    // a ?: b --> when(val $subj = a) { .... }
    // where `val $subj = a` has a fake source
    object WhenGeneratedSubject : FirFakeSourceElementKind()

    // list[0] -> list.get(0) where name reference will have a fake source element
    object ArrayAccessNameReference : FirFakeSourceElementKind()

    // super.foo() --> super<Supertype>.foo()
    // where `Supertype` has a fake source
    object SuperCallImplicitType : FirFakeSourceElementKind()

    // Consider `super<Supertype>.foo()`. The source PSI `Supertype` is referenced by both the qualified access expression
    // `super<Supertype>` and the calleeExpression `super<Supertype>`. To avoid having two FIR elements sharing the same source, this fake
    // source is assigned to the qualified access expression.
    object SuperCallExplicitType : FirFakeSourceElementKind()

    // fun foo(vararg args: Int) {}
    // fun bar(1, 2, 3) --> [resolved] fun bar(VarargArgument(1, 2, 3))
    object VarargArgument : FirFakeSourceElementKind()

    // Part of desugared x?.y
    object CheckedSafeCallSubject : FirFakeSourceElementKind()

    // { it + 1} --> { it -> it + 1 }
    // where `it` parameter declaration has fake source
    object ItLambdaParameter : FirFakeSourceElementKind()

    // for java annotations implicit constructor is generated
    // with a fake source which refers to containing class
    object ImplicitJavaAnnotationConstructor : FirFakeSourceElementKind()

    // for java annotations constructor implicit parameters are generated
    // with a fake source which refers to declared annotation methods
    object ImplicitAnnotationAnnotationConstructorParameter : FirFakeSourceElementKind()

    // for the implicit field storing the delegated object for class delegation
    // with a fake source that refers to the KtExpression that creates the delegate
    object ClassDelegationField : FirFakeSourceElementKind()
}

sealed class FirSourceElement {
    abstract val elementType: IElementType
    abstract val startOffset: Int
    abstract val endOffset: Int
    abstract val kind: FirSourceElementKind
    abstract val lighterASTNode: LighterASTNode
    abstract val treeStructure: FlyweightCapableTreeStructure<LighterASTNode>
}

// NB: in certain situations, psi.node could be null
// Potentially exceptions can be provoked by elementType / lighterASTNode
sealed class FirPsiSourceElement(val psi: PsiElement) : FirSourceElement() {
    override val elementType: IElementType
        get() = psi.node.elementType

    override val startOffset: Int
        get() = psi.textRange.startOffset

    override val endOffset: Int
        get() = psi.textRange.endOffset

    override val lighterASTNode by lazy { TreeBackedLighterAST.wrap(psi.node) }

    override val treeStructure: FlyweightCapableTreeStructure<LighterASTNode> by lazy { WrappedTreeStructure(psi.containingFile) }

    internal class WrappedTreeStructure(file: PsiFile) : FlyweightCapableTreeStructure<LighterASTNode> {
        private val lighterAST = TreeBackedLighterAST(file.node)

        fun unwrap(node: LighterASTNode) = lighterAST.unwrap(node)

        override fun toString(node: LighterASTNode): CharSequence = unwrap(node).text

        override fun getRoot(): LighterASTNode = lighterAST.root

        override fun getParent(node: LighterASTNode): LighterASTNode? =
            unwrap(node).psi.parent?.node?.let { TreeBackedLighterAST.wrap(it) }

        override fun getChildren(node: LighterASTNode, nodesRef: Ref<Array<LighterASTNode>>): Int {
            val psi = unwrap(node).psi
            val children = mutableListOf<PsiElement>()
            var child = psi.firstChild
            while (child != null) {
                children += child
                child = child.nextSibling
            }
            if (children.isEmpty()) {
                nodesRef.set(LighterASTNode.EMPTY_ARRAY)
            } else {
                nodesRef.set(children.map { TreeBackedLighterAST.wrap(it.node) }.toTypedArray())
            }
            return children.size
        }

        override fun disposeChildren(p0: Array<out LighterASTNode>?, p1: Int) {
        }

        override fun getStartOffset(node: LighterASTNode): Int {
            return getStartOffset(unwrap(node).psi)
        }

        private fun getStartOffset(element: PsiElement): Int {
            var child = element.firstChild
            if (child != null) {
                while (child is PsiComment || child is PsiWhiteSpace) {
                    child = child.nextSibling
                }
                if (child != null) {
                    return getStartOffset(child)
                }
            }
            return element.textRange.startOffset
        }

        override fun getEndOffset(node: LighterASTNode): Int {
            return getEndOffset(unwrap(node).psi)
        }

        private fun getEndOffset(element: PsiElement): Int {
            var child = element.lastChild
            if (child != null) {
                while (child is PsiComment || child is PsiWhiteSpace) {
                    child = child.prevSibling
                }
                if (child != null) {
                    return getEndOffset(child)
                }
            }
            return element.textRange.endOffset
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FirPsiSourceElement

        if (psi != other.psi) return false

        return true
    }

    override fun hashCode(): Int {
        return psi.hashCode()
    }
}

class FirRealPsiSourceElement(psi: PsiElement) : FirPsiSourceElement(psi) {
    override val kind: FirSourceElementKind get() = FirRealSourceElementKind
}

class FirFakeSourceElement(psi: PsiElement, override val kind: FirFakeSourceElementKind) : FirPsiSourceElement(psi) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as FirFakeSourceElement

        if (kind != other.kind) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + kind.hashCode()
        return result
    }
}

fun FirSourceElement.fakeElement(newKind: FirFakeSourceElementKind): FirSourceElement {
    return when (this) {
        is FirLightSourceElement -> FirLightSourceElement(lighterASTNode, startOffset, endOffset, treeStructure, newKind)
        is FirPsiSourceElement -> FirFakeSourceElement(psi, newKind)
    }
}

fun FirSourceElement.realElement(): FirSourceElement = when (this) {
    is FirRealPsiSourceElement -> this
    is FirLightSourceElement -> FirLightSourceElement(lighterASTNode, startOffset, endOffset, treeStructure, FirRealSourceElementKind)
    is FirPsiSourceElement -> FirRealPsiSourceElement(psi)
}


class FirLightSourceElement(
    override val lighterASTNode: LighterASTNode,
    override val startOffset: Int,
    override val endOffset: Int,
    override val treeStructure: FlyweightCapableTreeStructure<LighterASTNode>,
    override val kind: FirSourceElementKind = FirRealSourceElementKind,
) : FirSourceElement() {
    override val elementType: IElementType
        get() = lighterASTNode.tokenType

    /**
     * We can create a [FirLightSourceElement] from a [FirPsiSourceElement] by using [FirPsiSourceElement.lighterASTNode];
     * [unwrapToFirPsiSourceElement] allows to get original [FirPsiSourceElement] in such case.
     *
     * If it is `pure` [FirLightSourceElement], i.e, compiler created it in light tree mode, then return [unwrapToFirPsiSourceElement] `null`.
     * Otherwise, return some not-null result.
     */
    fun unwrapToFirPsiSourceElement(): FirPsiSourceElement? {
        if (treeStructure !is FirPsiSourceElement.WrappedTreeStructure) return null
        val node = treeStructure.unwrap(lighterASTNode)
        return node.psi?.toFirPsiSourceElement(kind)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FirLightSourceElement

        if (lighterASTNode != other.lighterASTNode) return false
        if (startOffset != other.startOffset) return false
        if (endOffset != other.endOffset) return false
        if (treeStructure != other.treeStructure) return false
        if (kind != other.kind) return false

        return true
    }

    override fun hashCode(): Int {
        var result = lighterASTNode.hashCode()
        result = 31 * result + startOffset
        result = 31 * result + endOffset
        result = 31 * result + treeStructure.hashCode()
        result = 31 * result + kind.hashCode()
        return result
    }
}

val FirSourceElement?.psi: PsiElement? get() = (this as? FirPsiSourceElement)?.psi

val FirSourceElement?.text: CharSequence?
    get() = when (this) {
        is FirPsiSourceElement -> psi.text
        is FirLightSourceElement -> treeStructure.toString(lighterASTNode)
        else -> null
    }

@Suppress("NOTHING_TO_INLINE")
inline fun PsiElement.toFirPsiSourceElement(kind: FirSourceElementKind = FirRealSourceElementKind): FirPsiSourceElement = when (kind) {
    is FirRealSourceElementKind -> FirRealPsiSourceElement(this)
    is FirFakeSourceElementKind -> FirFakeSourceElement(this, kind)
}

@Suppress("NOTHING_TO_INLINE")
inline fun LighterASTNode.toFirLightSourceElement(
    tree: FlyweightCapableTreeStructure<LighterASTNode>,
    kind: FirSourceElementKind = FirRealSourceElementKind,
    startOffset: Int = this.startOffset,
    endOffset: Int = this.endOffset
): FirLightSourceElement = FirLightSourceElement(this, startOffset, endOffset, tree, kind)
