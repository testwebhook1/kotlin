/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.tower

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.synthetic.SyntheticMemberDescriptor
import org.jetbrains.kotlin.psi.Call
import org.jetbrains.kotlin.psi.ValueArgument
import org.jetbrains.kotlin.resolve.calls.components.isVararg
import org.jetbrains.kotlin.resolve.calls.inference.components.FreshVariableNewTypeSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.components.NewTypeSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.components.NewTypeSubstitutorByConstructorMap
import org.jetbrains.kotlin.resolve.calls.inference.substitute
import org.jetbrains.kotlin.resolve.calls.inference.substituteAndApproximateTypes
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.calls.util.isNotSimpleCall
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeApproximator
import org.jetbrains.kotlin.types.isFlexible
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import org.jetbrains.kotlin.types.typeUtil.makeNullable
import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.kotlin.utils.addToStdlib.compactIfPossible

sealed class NewAbstractResolvedCall<D : CallableDescriptor> : ResolvedCall<D> {
    abstract val argumentMappingByOriginal: Map<ValueParameterDescriptor, ResolvedCallArgument>
    abstract val kotlinCall: KotlinCall?
    abstract val languageVersionSettings: LanguageVersionSettings
    abstract val resolvedCallAtom: ResolvedCallAtom?
    abstract val psiKotlinCall: PSIKotlinCall
    abstract val typeApproximator: TypeApproximator

    protected var argumentToParameterMap: Map<ValueArgument, ArgumentMatchImpl>? = null
    protected var _valueArguments: Map<ValueParameterDescriptor, ResolvedValueArgument>? = null

    private var nonTrivialUpdatedResultInfo: DataFlowInfo? = null

    protected abstract var _extensionReceiver: ReceiverValue?
    protected abstract var _dispatchReceiver: ReceiverValue?

    open var isCompleted: Boolean = false
        protected set

    abstract fun containsOnlyOnlyInputTypesErrors(): Boolean

    override fun getExtensionReceiver(): ReceiverValue? = _extensionReceiver
    override fun getDispatchReceiver(): ReceiverValue? = _dispatchReceiver

    override fun getCall(): Call = psiKotlinCall.psiCall

    override fun getValueArguments(): Map<ValueParameterDescriptor, ResolvedValueArgument> {
        if (_valueArguments == null) {
            _valueArguments = createValueArguments()
        }
        return _valueArguments!!
    }

    fun setValueArguments(m: Map<ValueParameterDescriptor, ResolvedValueArgument>) {
        _valueArguments = m
    }

    open fun setResultingSubstitutor(substitutor: NewTypeSubstitutor?) {}

    abstract val freshSubstitutor: FreshVariableNewTypeSubstitutor?

    private fun CallableDescriptor.substituteInferredVariablesAndApproximate(
        substitutor: NewTypeSubstitutor?,
        shouldApproximate: Boolean = true
    ): CallableDescriptor {
        val inferredTypeVariablesSubstitutor = substitutor ?: FreshVariableNewTypeSubstitutor.Empty

        val freshVariablesSubstituted = freshSubstitutor?.let(::substitute) ?: this
        val knownTypeParameterSubstituted = resolvedCallAtom?.knownParametersSubstitutor?.let(freshVariablesSubstituted::substitute)
            ?: freshVariablesSubstituted

        return knownTypeParameterSubstituted.substituteAndApproximateTypes(
            inferredTypeVariablesSubstitutor,
            typeApproximator = if (shouldApproximate) typeApproximator else null,
            positionDependentApproximation
        )
    }

    protected open val positionDependentApproximation = false

    protected fun substitutedResultingDescriptor(substitutor: NewTypeSubstitutor?) =
        when (val candidateDescriptor = candidateDescriptor) {
            is ClassConstructorDescriptor, is SyntheticMemberDescriptor<*> -> {
                val explicitTypeArguments = resolvedCallAtom?.atom?.typeArguments?.filterIsInstance<SimpleTypeArgument>() ?: emptyList()

                candidateDescriptor.substituteInferredVariablesAndApproximate(
                    getSubstitutorWithoutFlexibleTypes(substitutor, explicitTypeArguments),
                )
            }
            is FunctionDescriptor -> {
                candidateDescriptor.substituteInferredVariablesAndApproximate(substitutor, candidateDescriptor.isNotSimpleCall())
            }
            is PropertyDescriptor -> {
                if (candidateDescriptor.isNotSimpleCall()) {
                    candidateDescriptor.substituteInferredVariablesAndApproximate(substitutor)
                } else {
                    candidateDescriptor
                }
            }
            else -> candidateDescriptor
        }

    private fun getSubstitutorWithoutFlexibleTypes(
        currentSubstitutor: NewTypeSubstitutor?,
        explicitTypeArguments: List<SimpleTypeArgument>,
    ): NewTypeSubstitutor? {
        if (currentSubstitutor !is NewTypeSubstitutorByConstructorMap || explicitTypeArguments.isEmpty()) return currentSubstitutor
        if (!currentSubstitutor.map.any { (_, value) -> value.isFlexible() }) return currentSubstitutor

        val typeVariables = freshSubstitutor?.freshVariables ?: return null
        val newSubstitutorMap = currentSubstitutor.map.toMutableMap()

        explicitTypeArguments.forEachIndexed { index, typeArgument ->
            val typeVariableConstructor = typeVariables.getOrNull(index)?.freshTypeConstructor ?: return@forEachIndexed

            newSubstitutorMap[typeVariableConstructor] =
                newSubstitutorMap[typeVariableConstructor]?.withNullabilityFromExplicitTypeArgument(typeArgument)
                    ?: return@forEachIndexed
        }

        return NewTypeSubstitutorByConstructorMap(newSubstitutorMap)
    }

    private fun KotlinType.withNullabilityFromExplicitTypeArgument(typeArgument: SimpleTypeArgument) =
        (if (typeArgument.type.isMarkedNullable) makeNullable() else makeNotNullable()).unwrap()

    private fun updateDispatchReceiverType(newType: KotlinType) {
        if (_dispatchReceiver?.type == newType) return
        _dispatchReceiver = _dispatchReceiver?.replaceType(newType)
    }

    private fun updateExtensionReceiverType(newType: KotlinType) {
        if (_extensionReceiver?.type == newType) return
        _extensionReceiver = _extensionReceiver?.replaceType(newType)
    }

    fun substituteReceivers(substitutor: NewTypeSubstitutor?) {
        if (substitutor != null) {
            // todo: add asset that we do not complete call many times
            isCompleted = true

            _dispatchReceiver?.type?.let {
                val newType = substitutor.safeSubstitute(it.unwrap())
                updateDispatchReceiverType(newType)
            }

            _extensionReceiver?.type?.let {
                val newType = substitutor.safeSubstitute(it.unwrap())
                updateExtensionReceiverType(newType)
            }
        }
    }

    override fun getValueArgumentsByIndex(): List<ResolvedValueArgument>? {
        val arguments = ArrayList<ResolvedValueArgument?>(candidateDescriptor.valueParameters.size)
        for (i in 0 until candidateDescriptor.valueParameters.size) {
            arguments.add(null)
        }

        for ((parameterDescriptor, value) in valueArguments) {
            val oldValue = arguments.set(parameterDescriptor.index, value)
            if (oldValue != null) {
                return null
            }
        }

        if (arguments.any { it == null }) return null

        @Suppress("UNCHECKED_CAST")
        return arguments as List<ResolvedValueArgument>
    }

    override fun getArgumentMapping(valueArgument: ValueArgument): ArgumentMapping {
        if (argumentToParameterMap == null) {
            argumentToParameterMap = argumentToParameterMap(resultingDescriptor, valueArguments)
        }
        return argumentToParameterMap!![valueArgument] ?: ArgumentUnmapped
    }

    override fun getDataFlowInfoForArguments() = object : DataFlowInfoForArguments {
        override fun getResultInfo(): DataFlowInfo = nonTrivialUpdatedResultInfo ?: psiKotlinCall.resultDataFlowInfo

        override fun getInfo(valueArgument: ValueArgument): DataFlowInfo {
            val externalPsiCallArgument = kotlinCall?.externalArgument?.psiCallArgument
            if (externalPsiCallArgument?.valueArgument == valueArgument) {
                return externalPsiCallArgument.dataFlowInfoAfterThisArgument
            }
            return psiKotlinCall.dataFlowInfoForArguments.getInfo(valueArgument)
        }
    }

    // Currently, updated only with info from effect system
    internal fun updateResultingDataFlowInfo(dataFlowInfo: DataFlowInfo) {
        if (dataFlowInfo == DataFlowInfo.EMPTY) return
        assert(nonTrivialUpdatedResultInfo == null) {
            "Attempt to rewrite resulting dataFlowInfo enhancement for call: $kotlinCall"
        }
        nonTrivialUpdatedResultInfo = dataFlowInfo.and(psiKotlinCall.resultDataFlowInfo)
    }

    abstract fun argumentToParameterMap(
        resultingDescriptor: CallableDescriptor,
        valueArguments: Map<ValueParameterDescriptor, ResolvedValueArgument>,
    ): Map<ValueArgument, ArgumentMatchImpl>

    private fun createValueArguments(): Map<ValueParameterDescriptor, ResolvedValueArgument> =
        LinkedHashMap<ValueParameterDescriptor, ResolvedValueArgument>().also { result ->
            val needToUseCorrectExecutionOrderForVarargArguments =
                languageVersionSettings.supportsFeature(LanguageFeature.UseCorrectExecutionOrderForVarargArguments)
            var varargMappings: MutableList<Pair<ValueParameterDescriptor, VarargValueArgument>>? = null
            for ((originalParameter, resolvedCallArgument) in argumentMappingByOriginal) {
                val resultingParameter = resultingDescriptor.valueParameters[originalParameter.index]

                result[resultingParameter] = when (resolvedCallArgument) {
                    ResolvedCallArgument.DefaultArgument ->
                        DefaultValueArgument.DEFAULT
                    is ResolvedCallArgument.SimpleArgument -> {
                        val valueArgument = resolvedCallArgument.callArgument.psiCallArgument.valueArgument
                        if (resultingParameter.isVararg) {
                            if (needToUseCorrectExecutionOrderForVarargArguments) {
                                VarargValueArgument().apply { addArgument(valueArgument) }
                            } else {
                                val vararg = VarargValueArgument().apply { addArgument(valueArgument) }
                                if (varargMappings == null) varargMappings = SmartList()
                                varargMappings.add(resultingParameter to vararg)
                                continue
                            }
                        } else {
                            ExpressionValueArgument(valueArgument)
                        }
                    }
                    is ResolvedCallArgument.VarargArgument ->
                        VarargValueArgument().apply {
                            resolvedCallArgument.arguments.map { it.psiCallArgument.valueArgument }.forEach { addArgument(it) }
                        }
                }
            }

            if (varargMappings != null && !needToUseCorrectExecutionOrderForVarargArguments) {
                for ((parameter, argument) in varargMappings) {
                    result[parameter] = argument
                }
            }
        }.compactIfPossible()
}