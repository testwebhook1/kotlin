/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.backend.common.lower
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.common.phaser.invokeToplevel
import org.jetbrains.kotlin.ir.backend.js.ic.ModuleCache
import org.jetbrains.kotlin.ir.backend.js.ic.icCompile
import org.jetbrains.kotlin.ir.backend.js.lower.generateTests
import org.jetbrains.kotlin.ir.backend.js.lower.moveBodilessDeclarationsToSeparatePlace
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.IrModuleToJsTransformer
import org.jetbrains.kotlin.ir.backend.js.utils.NameTables
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.StageController
import org.jetbrains.kotlin.ir.declarations.persistent.PersistentIrFactory
import org.jetbrains.kotlin.ir.util.ExternalDependenciesGenerator
import org.jetbrains.kotlin.ir.util.noUnboundLeft
import org.jetbrains.kotlin.js.config.RuntimeDiagnostic
import org.jetbrains.kotlin.name.FqName

class CompilerResult(
    val outputs: CompilationOutputs?,
    val outputsAfterDce: CompilationOutputs?,
    val tsDefinitions: String? = null
)

class CompilationOutputs(
    val jsCode: String,
    val sourceMap: String? = null,
    val dependencies: Iterable<Pair<String, CompilationOutputs>> = emptyList()
)

fun compile(
    depsDescriptors: ModulesStructure,
    phaseConfig: PhaseConfig,
    irFactory: IrFactory,
    mainArguments: List<String>?,
    exportedDeclarations: Set<FqName> = emptySet(),
    generateFullJs: Boolean = true,
    generateDceJs: Boolean = false,
    dceDriven: Boolean = false,
    dceRuntimeDiagnostic: RuntimeDiagnostic? = null,
    es6mode: Boolean = false,
    multiModule: Boolean = false,
    relativeRequirePath: Boolean = false,
    propertyLazyInitialization: Boolean,
    verifySignatures: Boolean = true,
    legacyPropertyAccess: Boolean = false,
    baseClassIntoMetadata: Boolean = false,
    lowerPerModule: Boolean = false,
    safeExternalBoolean: Boolean = false,
    safeExternalBooleanDiagnostic: RuntimeDiagnostic? = null,
): CompilerResult {

    if (lowerPerModule) {
        return icCompile(
            depsDescriptors,
            mainArguments,
            exportedDeclarations,
            generateFullJs,
            generateDceJs,
            dceRuntimeDiagnostic,
            es6mode,
            multiModule,
            relativeRequirePath,
            propertyLazyInitialization,
            baseClassIntoMetadata,
            legacyPropertyAccess,
            safeExternalBoolean,
            safeExternalBooleanDiagnostic,
        )
    }

    val (moduleFragment: IrModuleFragment, dependencyModules, irBuiltIns, symbolTable, deserializer, moduleToName) =
        loadIr(depsDescriptors, irFactory, verifySignatures)
    val mainModule = depsDescriptors.mainModule
    val configuration = depsDescriptors.compilerConfiguration

    val moduleDescriptor = moduleFragment.descriptor

    val allModules = when (mainModule) {
        is MainModule.SourceFiles -> dependencyModules + listOf(moduleFragment)
        is MainModule.Klib -> dependencyModules
    }

    val context = JsIrBackendContext(
        moduleDescriptor,
        irBuiltIns,
        symbolTable,
        allModules.first(),
        exportedDeclarations,
        configuration,
        es6mode = es6mode,
        dceRuntimeDiagnostic = dceRuntimeDiagnostic,
        propertyLazyInitialization = propertyLazyInitialization,
        legacyPropertyAccess = legacyPropertyAccess,
        baseClassIntoMetadata = baseClassIntoMetadata,
        safeExternalBoolean = safeExternalBoolean,
        safeExternalBooleanDiagnostic = safeExternalBooleanDiagnostic
    )

    // Load declarations referenced during `context` initialization
    val irProviders = listOf(deserializer)
    ExternalDependenciesGenerator(symbolTable, irProviders).generateUnboundSymbolsAsDependencies()

    deserializer.postProcess()
    symbolTable.noUnboundLeft("Unbound symbols at the end of linker")

    allModules.forEach { module ->
        moveBodilessDeclarationsToSeparatePlace(context, module)
    }

    // TODO should be done incrementally
    generateTests(context, allModules.last())

    if (dceDriven) {
        val controller = MutableController(context, pirLowerings)

        check(irFactory is PersistentIrFactory)
        irFactory.stageController = controller

        controller.currentStage = controller.lowerings.size + 1

        eliminateDeadDeclarations(allModules, context)

        irFactory.stageController = StageController(controller.currentStage)

        val transformer = IrModuleToJsTransformer(
            context,
            mainArguments,
            fullJs = true,
            dceJs = false,
            multiModule = multiModule,
            relativeRequirePath = relativeRequirePath,
            moduleToName = moduleToName,
        )
        return transformer.generateModule(allModules)
    } else {
        // TODO is this reachable when lowerPerModule == true?
        if (lowerPerModule) {
            val controller = WholeWorldStageController()
            check(irFactory is PersistentIrFactory)
            irFactory.stageController = controller
            allModules.forEach {
                lowerPreservingIcData(it, context, controller)
            }
            irFactory.stageController = object : StageController(irFactory.stageController.currentStage) {}
        } else {
            jsPhases.invokeToplevel(phaseConfig, context, allModules)
        }

        val transformer = IrModuleToJsTransformer(
            context,
            mainArguments,
            fullJs = generateFullJs,
            dceJs = generateDceJs,
            multiModule = multiModule,
            relativeRequirePath = relativeRequirePath,
            moduleToName = moduleToName,
        )
        return transformer.generateModule(allModules)
    }
}

fun lowerPreservingIcData(module: IrModuleFragment, context: JsIrBackendContext, controller: WholeWorldStageController) {
    // Lower all the things
    controller.currentStage = 0

    pirLowerings.forEachIndexed { i, lowering ->
        controller.currentStage = i + 1
        when (lowering) {
            is DeclarationLowering ->
                lowering.declarationTransformer(context).lower(module)
            is BodyLowering ->
                lowering.bodyLowering(context).lower(module)
            is ModuleLowering -> { /*TODO what about other lowerings? */ }
        }
    }

    controller.currentStage = pirLowerings.size + 1
}

@Suppress("UNUSED_PARAMETER")
fun generateJsFromAst(
    mainModule: String,
    caches: Map<String, ModuleCache>
): CompilerResult {
    TODO(">> EP to generate Js from BinaryAST with root $mainModule")
}

fun generateJsCode(
    context: JsIrBackendContext,
    moduleFragment: IrModuleFragment,
    nameTables: NameTables
): String {
    moveBodilessDeclarationsToSeparatePlace(context, moduleFragment)
    jsPhases.invokeToplevel(PhaseConfig(jsPhases), context, listOf(moduleFragment))

    val transformer = IrModuleToJsTransformer(context, null, true, nameTables)
    return transformer.generateModule(listOf(moduleFragment)).outputs!!.jsCode
}