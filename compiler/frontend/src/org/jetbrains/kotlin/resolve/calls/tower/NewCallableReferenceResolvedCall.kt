/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.tower

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.psi.ValueArgument
import org.jetbrains.kotlin.resolve.calls.inference.components.FreshVariableNewTypeSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.components.NewTypeSubstitutor
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.calls.util.toResolutionStatus
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.types.*

class NewCallableReferenceResolvedCall<D : CallableDescriptor>(
    val resolvedAtom: ResolvedCallableReferenceAtom,
    override val typeApproximator: TypeApproximator,
    override val languageVersionSettings: LanguageVersionSettings,
    substitutor: NewTypeSubstitutor? = null,
) : NewAbstractResolvedCall<D>() {
    private lateinit var resultingDescriptor: D

    override var _extensionReceiver = when (resolvedAtom) {
        is ResolvedCallableReferenceCallAtom -> resolvedAtom.extensionReceiverArgument?.receiverValue
        is ResolvedCallableReferenceArgumentAtom -> resolvedAtom.candidate?.extensionReceiver?.receiver?.receiverValue
    }

    override var _dispatchReceiver = when (resolvedAtom) {
        is ResolvedCallableReferenceCallAtom -> resolvedAtom.dispatchReceiverArgument?.receiverValue
        is ResolvedCallableReferenceArgumentAtom -> resolvedAtom.candidate?.dispatchReceiver?.receiver?.receiverValue
    }

    override val positionDependentApproximation: Boolean = true

    var diagnostics: Collection<KotlinCallDiagnostic> = mutableListOf()

    override val resolvedCallAtom = when (resolvedAtom) {
        is ResolvedCallableReferenceCallAtom -> resolvedAtom
        is ResolvedCallableReferenceArgumentAtom -> resolvedAtom.candidate?.resolvedCall
    }

    override val psiKotlinCall = when (resolvedAtom) {
        is ResolvedCallableReferenceCallAtom -> resolvedAtom.atom.psiKotlinCall
        is ResolvedCallableReferenceArgumentAtom -> resolvedAtom.atom.call.psiKotlinCall
    }

    @Suppress("UNCHECKED_CAST")
    override fun getCandidateDescriptor(): D = when (resolvedAtom) {
        is ResolvedCallableReferenceCallAtom -> resolvedAtom.candidateDescriptor as D
        is ResolvedCallableReferenceArgumentAtom -> resolvedAtom.candidate?.candidate as D
    }

    override fun getResultingDescriptor(): D = resultingDescriptor
    override fun getExtensionReceiver(): ReceiverValue? = _extensionReceiver
    override fun getDispatchReceiver(): ReceiverValue? = _dispatchReceiver
    override fun getValueArguments() = emptyMap<ValueParameterDescriptor, ResolvedValueArgument>()
    override fun getArgumentMapping(valueArgument: ValueArgument): ArgumentMapping = ArgumentUnmapped
    override fun getTypeArguments() = emptyMap<TypeParameterDescriptor, KotlinType>()
    override fun getStatus() = getResultApplicability(diagnostics).toResolutionStatus()

    override fun getExplicitReceiverKind() = when (resolvedAtom) {
        is ResolvedCallableReferenceArgumentAtom ->
            resolvedAtom.candidate?.explicitReceiverKind ?: ExplicitReceiverKind.NO_EXPLICIT_RECEIVER
        is ResolvedCallableReferenceCallAtom -> resolvedAtom.explicitReceiverKind
    }

    override fun getDataFlowInfoForArguments(): DataFlowInfoForArguments =
        MutableDataFlowInfoForArguments.WithoutArgumentsCheck(DataFlowInfo.EMPTY)

    override fun getSmartCastDispatchReceiverType(): KotlinType? = null

    override fun setResultingSubstitutor(substitutor: NewTypeSubstitutor?) {
        substituteReceivers(substitutor)

        @Suppress("UNCHECKED_CAST")
        resultingDescriptor = substitutedResultingDescriptor(substitutor) as D
    }

    override val freshSubstitutor: FreshVariableNewTypeSubstitutor? =
        when (resolvedAtom) {
            is ResolvedCallableReferenceCallAtom -> resolvedAtom.freshVariablesSubstitutor
            is ResolvedCallableReferenceArgumentAtom -> resolvedAtom.candidate?.freshVariablesSubstitutor
        }

    override val argumentMappingByOriginal: Map<ValueParameterDescriptor, ResolvedCallArgument> = emptyMap()

    override val kotlinCall: KotlinCall? = when (resolvedAtom) {
        is ResolvedCallableReferenceArgumentAtom -> resolvedAtom.candidate?.kotlinCall?.call
        is ResolvedCallableReferenceCallAtom -> resolvedAtom.atom
    }

    override fun containsOnlyOnlyInputTypesErrors(): Boolean = false

    override fun argumentToParameterMap(
        resultingDescriptor: CallableDescriptor,
        valueArguments: Map<ValueParameterDescriptor, ResolvedValueArgument>
    ): Map<ValueArgument, ArgumentMatchImpl> = emptyMap()

    init {
        setResultingSubstitutor(substitutor)
    }
}