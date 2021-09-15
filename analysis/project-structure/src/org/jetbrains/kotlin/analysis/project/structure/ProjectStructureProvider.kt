/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.project.structure

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement

public abstract class ProjectStructureProvider {
    /**
     * For a given [PsiElement] get a [ProjectModule] to which [PsiElement] belongs.
     */
    public abstract fun getProjectModuleForKtElement(element: PsiElement): ProjectModule
}

/**
 * For a given [PsiElement] get a [ProjectModule] to which [PsiElement] belongs.
 * @param project [Project] which contains current [PsiElement]. `PsiElement.project` may be a heavy operation as it includes PSI tree traversal. So, when a [Project] is  already available, it is better to pass it explicitly
 */
public fun PsiElement.getProjectModule(project: Project = this.project): ProjectModule =
    ServiceManager.getService(project, ProjectStructureProvider::class.java)
        .getProjectModuleForKtElement(this)

/**
 * For a given [PsiElement] get a [ProjectModule] to which [PsiElement] belongs.
 * @return [ProjectModule] of type [M] if `result <: M`, [java.lang.ClassCastException] otherwise
 * @param project [Project] which contains current [PsiElement]. `PsiElement.project` may be a heavy operation as it includes PSI tree traversal. So, when a [Project] is  already available, it is better to pass it explicitly
 */
public inline fun <reified M : ProjectModule> PsiElement.getProjectModuleOfType(project: Project = this.project): M =
    getProjectModule(project) as M

/**
 * For a given [PsiElement] get a [ProjectModule] to which [PsiElement] belongs.
 * @return [ProjectModule] of type [M] if `result <: M`, `null` otherwise
 * @param project [Project] which contains current [PsiElement]. `PsiElement.project` may be a heavy operation as it includes PSI tree traversal. So, when a [Project] is  already available, it is better to pass it explicitly
 */
public inline fun <reified M : ProjectModule> PsiElement.getProjectModuleOfTypeSafe(project: Project = this.project): M? =
    getProjectModule(project) as M?