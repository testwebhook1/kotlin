import org.jetbrains.kotlin.ideaExt.idea
import java.io.File

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

val compilerModules: Array<String> by rootProject.extra
val otherCompilerModules = compilerModules.filter { it != path }

val antLauncherJar by configurations.creating

dependencies {
    testRuntime(intellijDep()) // Should come before compiler, because of "progarded" stuff needed for tests

    testCompile(project(":kotlin-script-runtime"))
    testCompile(project(":kotlin-test:kotlin-test-jvm"))
    
    testCompile(kotlinStdlib())

    testCompile(commonDep("junit:junit"))
    testCompileOnly(project(":kotlin-test:kotlin-test-jvm"))
    testCompileOnly(project(":kotlin-test:kotlin-test-junit"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(projectTests(":compiler:fir:raw-fir:psi2fir"))
    testCompile(projectTests(":compiler:fir:raw-fir:light-tree2fir"))
    testCompile(projectTests(":compiler:fir:fir2ir"))
    testCompile(projectTests(":compiler:fir:analysis-tests:legacy-fir-tests"))
    testCompile(projectTests(":generators:test-generator"))
    testCompile(project(":compiler:ir.ir2cfg"))
    testCompile(project(":compiler:ir.tree")) // used for deepCopyWithSymbols call that is removed by proguard from the compiler TODO: make it more straightforward
    testCompile(project(":kotlin-scripting-compiler"))
    testCompile(project(":kotlin-script-util"))
    testCompileOnly(project(":kotlin-reflect-api"))
    otherCompilerModules.forEach {
        testCompileOnly(project(it))
    }
    testCompileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    testCompileOnly(intellijDep()) { includeJars("idea", "idea_rt", "util", "asm-all", rootProject = rootProject) }

    testRuntimeOnly(intellijPluginDep("java"))

    testRuntime(project(":kotlin-reflect"))
    testRuntime(toolsJar())

    antLauncherJar(commonDep("org.apache.ant", "ant"))
    antLauncherJar(toolsJar())
}

val generationRoot = projectDir.resolve("tests-gen")

sourceSets {
    "main" {}
    "test" {
        projectDefault()
        this.java.srcDir(generationRoot.name)
    }
}

if (kotlinBuildProperties.isInJpsBuildIdeaSync) {
    apply(plugin = "idea")
    idea {
        this.module.generatedSourceDirs.add(generationRoot)
    }
}

projectTest(parallel = true) {
    dependsOn(":dist")

    workingDir = rootDir
    systemProperty("kotlin.test.script.classpath", testSourceSet.output.classesDirs.joinToString(File.pathSeparator))
    val antLauncherJarPathProvider = project.provider {
        antLauncherJar.asPath
    }
    doFirst {
        systemProperty("kotlin.ant.classpath", antLauncherJarPathProvider.get())
        systemProperty("kotlin.ant.launcher.class", "org.apache.tools.ant.Main")
    }
}

val generateTestData by generator("org.jetbrains.kotlin.generators.tests.GenerateCompilerTestDataKt")

testsJar()
