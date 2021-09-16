/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner

import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkQueue
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.build.report.metrics.BuildMetricsReporter
import org.jetbrains.kotlin.gradle.tasks.GradleCompileTaskProvider
import org.jetbrains.kotlin.gradle.tasks.TaskOutputsBackup
import java.io.File

/**
 * Uses Gradle worker api to run kotlin compilation.
 */
internal class GradleCompilerRunnerWithWorkers(
    taskProvider: GradleCompileTaskProvider,
    jdkToolsJar: File?,
    kotlinDaemonJvmArgs: List<String>?,
    buildMetrics: BuildMetricsReporter,
    private val workerExecutor: WorkerExecutor
) : GradleCompilerRunner(taskProvider, jdkToolsJar, kotlinDaemonJvmArgs, buildMetrics) {
    override fun runCompilerAsync(
        workArgs: GradleKotlinCompilerWorkArguments,
        taskOutputsBackup: TaskOutputsBackup?
    ): WorkQueue {

        val workQueue = workerExecutor.noIsolation()
        workQueue.submit(GradleKotlinCompilerWorkAction::class.java) {
            it.compilerWorkArguments.set(workArgs)
            if (taskOutputsBackup != null) {
                it.taskOutputs.from(taskOutputsBackup.outputs)
                it.taskOutputsSnapshot.set(taskOutputsBackup.previousOutputs)
            }
        }
        return workQueue
    }

    internal abstract class GradleKotlinCompilerWorkAction
        : WorkAction<GradleKotlinCompilerWorkParameters> {

        override fun execute() {
            try {
                GradleKotlinCompilerWork(
                    parameters.compilerWorkArguments.get()
                ).run()
            } catch (e: GradleException) {
                if (parameters.taskOutputsSnapshot.isPresent) {
                    val taskOutputsBackup = TaskOutputsBackup(
                        parameters.taskOutputs,
                        parameters.taskOutputsSnapshot.get()
                    )
                    // Measuring in worker task outputs restore time will not work
                    // Currently build metrics are collected and persisted right after task action execution
                    // while worker could be executed at a later time
                    taskOutputsBackup.restoreOutputs()
                }

                throw e
            }
        }
    }

    internal interface GradleKotlinCompilerWorkParameters : WorkParameters {
        val compilerWorkArguments: Property<GradleKotlinCompilerWorkArguments>
        val taskOutputs: ConfigurableFileCollection
        val taskOutputsSnapshot: MapProperty<File, Array<Byte>>
    }
}