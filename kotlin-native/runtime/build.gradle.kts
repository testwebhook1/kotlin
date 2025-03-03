/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.testing.native.*
import org.jetbrains.kotlin.bitcode.CompileToBitcode
import org.jetbrains.kotlin.konan.target.*
import org.jetbrains.kotlin.konan.target.Architecture as TargetArchitecture

plugins {
    id("compile-to-bitcode")
    id("runtime-testing")
}

googletest {
    revision = project.property("gtestRevision") as String
    refresh = project.hasProperty("refresh-gtest")
}

fun CompileToBitcode.includeRuntime() {
    headersDirs += files("src/main/cpp")
}

val hostName: String by project
val targetList: List<String> by project

bitcode {
    create("runtime", file("src/main")) {
        dependsOn(
            ":kotlin-native:dependencies:update",
            "${target}StdAlloc",
            "${target}OptAlloc",
            "${target}Mimalloc",
            "${target}Libbacktrace",
            "${target}Launcher",
            "${target}Debug",
            "${target}SourceInfoCoreSymbolication",
            "${target}SourceInfoLibbacktrace",
            "${target}Strict",
            "${target}Relaxed",
            "${target}ProfileRuntime",
            "${target}Objc",
            "${target}ExceptionsSupport",
            "${target}LegacyMemoryManager",
            "${target}ExperimentalMemoryManagerNoop",
            "${target}ExperimentalMemoryManagerStms",
            "${target}CommonGc",
            "${target}SameThreadMsGc",
            "${target}NoopGc"
        )
        includeRuntime()
    }

    create("mimalloc") {
        language = CompileToBitcode.Language.C
        includeFiles = listOf("**/*.c")
        excludeFiles += listOf("**/alloc-override*.c", "**/page-queue.c", "**/static.c", "**/bitmap.inc.c")
        srcDirs = files("$srcRoot/c")
        compilerArgs.addAll(listOf("-DKONAN_MI_MALLOC=1", "-Wno-unknown-pragmas", "-ftls-model=initial-exec",
                "-Wno-unused-function", "-Wno-error=atomic-alignment",
                "-Wno-unused-parameter" /* for windows 32*/))
        headersDirs = files("$srcRoot/c/include")

        onlyIf { targetSupportsMimallocAllocator(target) }
    }

    create("libbacktrace") {
        val targetInfo = HostManager().targetByName(target)
        language = CompileToBitcode.Language.C
        val useMachO = targetInfo.family.isAppleFamily
        val useElf = targetInfo.family in listOf(Family.LINUX, Family.ANDROID)
        includeFiles = listOfNotNull(
                "atomic.c",
                "backtrace.c",
                "dwarf.c",
                "elf.c".takeIf { useElf },
                "fileline.c",
                "macho.c".takeIf { useMachO },
                "mmap.c",
                "mmapio.c",
                "posix.c",
                "print.c",
                "simple.c",
                "sort.c",
                "state.c"
        )
        srcDirs = files("$srcRoot/c")
        val elfSize = when (targetInfo.architecture) {
            TargetArchitecture.X64, TargetArchitecture.ARM64 -> 64
            TargetArchitecture.X86, TargetArchitecture.ARM32,
            TargetArchitecture.MIPS32, TargetArchitecture.MIPSEL32,
            TargetArchitecture.WASM32 -> 32
        }
        compilerArgs.addAll(listOfNotNull(
                "-funwind-tables",
                "-W", "-Wall", "-Wwrite-strings", "-Wstrict-prototypes", "-Wmissing-prototypes",
                "-Wold-style-definition", "-Wmissing-format-attribute", "-Wcast-qual", "-O2",
                "-DBACKTRACE_ELF_SIZE=$elfSize".takeIf { useElf }
        ))
        headersDirs = files("$srcRoot/c/include")

        onlyIf { targetSupportsLibBacktrace(target) }
    }


    create("launcher") {
        includeRuntime()
    }

    create("debug") {
        includeRuntime()
    }

    create("std_alloc")
    create("opt_alloc")

    create("exceptionsSupport", file("src/exceptions_support")) {
        includeRuntime()
    }

    create("source_info_core_symbolication", file("src/source_info/core_symbolication")) {
        includeRuntime()
    }
    create("source_info_libbacktrace", file("src/source_info/libbacktrace")) {
        includeRuntime()
        headersDirs += files("src/libbacktrace/c/include")
        onlyIf { targetSupportsLibBacktrace(target) }
    }

    create("strict") {
        includeRuntime()
    }

    create("relaxed") {
        includeRuntime()
    }

    create("profileRuntime", file("src/profile_runtime"))

    create("objc") {
        includeRuntime()
    }

    create("test_support", outputGroup = "test") {
        includeRuntime()
        dependsOn("downloadGoogleTest")
        headersDirs += googletest.headersDirs
    }

    create("legacy_memory_manager", file("src/legacymm")) {
        includeRuntime()
    }

    create("experimental_memory_manager_noop", file("src/mm")) {
        headersDirs += files("src/gc/noop/cpp", "src/gc/common/cpp")
        includeRuntime()
    }

    create("experimental_memory_manager_stms", file("src/mm")) {
        headersDirs += files("src/gc/stms/cpp", "src/gc/common/cpp")
        includeRuntime()
    }

    create("common_gc", file("src/gc/common")) {
        headersDirs += files("src/mm/cpp")
        includeRuntime()
    }

    create("noop_gc", file("src/gc/noop")) {
        headersDirs += files("src/gc/noop/cpp", "src/gc/common/cpp", "src/mm/cpp")
        includeRuntime()
    }

    create("same_thread_ms_gc", file("src/gc/stms")) {
        headersDirs += files("src/gc/stms/cpp", "src/gc/common/cpp", "src/mm/cpp")
        includeRuntime()
    }
}

targetList.forEach { targetName ->
    val allTests = mutableListOf<Task>()

    allTests.addAll(createTestTasks(
            project,
            targetName,
            "${targetName}StdAllocRuntimeTests",
            listOf(
                "${targetName}Runtime",
                "${targetName}LegacyMemoryManager",
                "${targetName}Strict",
                "${targetName}StdAlloc",
                "${targetName}Objc"
            )
    ) {
        includeRuntime()
    })

    allTests.addAll(createTestTasks(
            project,
            targetName,
            "${targetName}MimallocRuntimeTests",
            listOf(
                "${targetName}Runtime",
                "${targetName}LegacyMemoryManager",
                "${targetName}Strict",
                "${targetName}Mimalloc",
                "${targetName}OptAlloc",
                "${targetName}Objc"
            )
    ) {
        includeRuntime()
    })

    allTests.addAll(createTestTasks(
            project,
            targetName,
            "${targetName}ExperimentalMMMimallocRuntimeTests",
            listOf(
                "${targetName}Runtime",
                "${targetName}ExperimentalMemoryManagerStms",
                "${targetName}CommonGc",
                "${targetName}SameThreadMsGc",
                "${targetName}Mimalloc",
                "${targetName}OptAlloc",
                "${targetName}Objc"
            )
    ) {
        headersDirs += files("src/gc/stms/cpp", "src/gc/common/cpp", "src/mm/cpp")
        includeRuntime()
    })

    allTests.addAll(createTestTasks(
            project,
            targetName,
            "${targetName}ExperimentalMMStdAllocRuntimeTests",
            listOf(
                "${targetName}Runtime",
                "${targetName}ExperimentalMemoryManagerStms",
                "${targetName}CommonGc",
                "${targetName}SameThreadMsGc",
                "${targetName}StdAlloc",
                "${targetName}Objc"
            )
    ) {
        headersDirs += files("src/gc/stms/cpp", "src/gc/common/cpp", "src/mm/cpp")
        includeRuntime()
    })

    allTests.addAll(createTestTasks(
            project,
            targetName,
            "${targetName}ExperimentalMMNoOpMimallocRuntimeTests",
            listOf(
                "${targetName}Runtime",
                "${targetName}ExperimentalMemoryManagerNoop",
                "${targetName}CommonGc",
                "${targetName}NoopGc",
                "${targetName}Mimalloc",
                "${targetName}OptAlloc",
                "${targetName}Objc"
            )
    ) {
        headersDirs += files("src/gc/noop/cpp", "src/gc/common/cpp", "src/mm/cpp")
        includeRuntime()
    })

    allTests.addAll(createTestTasks(
            project,
            targetName,
            "${targetName}ExperimentalMMNoOpStdAllocRuntimeTests",
            listOf(
                "${targetName}Runtime",
                "${targetName}ExperimentalMemoryManagerNoop",
                "${targetName}CommonGc",
                "${targetName}NoopGc",
                "${targetName}StdAlloc",
                "${targetName}Objc"
            )
    ) {
        headersDirs += files("src/gc/noop/cpp", "src/gc/common/cpp", "src/mm/cpp")
        includeRuntime()
    })

    // TODO: This "all tests" tasks should be provided by `CompileToBitcodeExtension`
    tasks.register("${targetName}RuntimeTests") {
        dependsOn(allTests)
    }
}

val hostRuntime by tasks.registering {
    dependsOn("${hostName}Runtime")
}

val hostRuntimeTests by tasks.registering {
    dependsOn("${hostName}RuntimeTests")
}

val hostStdAllocRuntimeTests by tasks.registering {
    dependsOn("${hostName}StdAllocRuntimeTests")
}

val hostMimallocRuntimeTests by tasks.registering {
    dependsOn("${hostName}MimallocRuntimeTests")
}

val hostExperimentalMMStdAllocRuntimeTests by tasks.registering {
    dependsOn("${hostName}ExperimentalMMStdAllocRuntimeTests")
}

val hostExperimentalMMMimallocRuntimeTests by tasks.registering {
    dependsOn("${hostName}ExperimentalMMMimallocRuntimeTests")
}

val assemble by tasks.registering {
    dependsOn(tasks.withType(CompileToBitcode::class).matching {
        it.outputGroup == "main"
    })
}

val hostAssemble by tasks.registering {
    dependsOn(tasks.withType(CompileToBitcode::class).matching {
        it.outputGroup == "main" && it.target == hostName
    })
}

val clean by tasks.registering {
    doFirst {
        delete(buildDir)
    }
}

val generateJsMath by tasks.registering {
    dependsOn(":distCompiler")
    doLast {
        val distDir: File by project
        val jsinteropScript = if (PlatformInfo.isWindows()) "jsinterop.bat" else "jsinterop"
        val jsinterop = "$distDir/bin/$jsinteropScript"
        val targetDir = "$buildDir/generated"

        project.exec {
            commandLine(
                    jsinterop,
                    "-pkg", "kotlinx.interop.wasm.math",
                    "-o", "$targetDir/math",
                    "-target", "wasm32"
            )
        }

        val generated = file("$targetDir/math-build/natives/js_stubs.js")
        val mathJs = file("src/main/js/math.js")
        mathJs.writeText(
            "// NOTE: THIS FILE IS AUTO-GENERATED!\n" +
            "// Run ':runtime:generateJsMath' to re-generate it.\n\n"
        )
        mathJs.appendText(generated.readText())
    }
}
