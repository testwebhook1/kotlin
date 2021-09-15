/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.project.structure

/**
 * A list of all modules current module can depend onwith regular dependency
 *
 * @see ProjectModule.regularDependencies
 */
public inline fun <reified M : ProjectModule> ProjectModule.regularDependenceisOfType(): Sequence<M> =
    regularDependencies.asSequence().filterIsInstance<M>()

/**
 * A list of all other modules current module can depend on.
 *
 * @see ProjectModule.regularDependencies
 * @see ProjectModule.refinementDependencies
 * @see ProjectModule.friendDependencies
 */
public fun ProjectModule.allDependencies(): List<ProjectModule> =
    buildList {
        addAll(regularDependencies)
        addAll(refinementDependencies)
        addAll(friendDependencies)
    }

/**
 * A list of all other modules of type [M] current module can depend on.
 *
 * @see ProjectModule.regularDependencies
 * @see ProjectModule.refinementDependencies
 * @see ProjectModule.friendDependencies
 */
public inline fun <reified M : ProjectModule> ProjectModule.allDependenciesOfType(): Sequence<M> =
    sequence {
        yieldAll(regularDependencies)
        yieldAll(refinementDependencies)
        yieldAll(friendDependencies)
    }.filterIsInstance<M>()