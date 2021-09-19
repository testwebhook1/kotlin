plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(project(":compiler:psi"))
    implementation(project(":analysis:project-structure"))
    compile(project(":compiler:fir:fir2ir"))
    compile(project(":compiler:fir:fir2ir:jvm-backend"))
    compile(project(":compiler:ir.serialization.common"))
    compile(project(":compiler:fir:resolve"))
    compile(project(":compiler:fir:checkers"))
    compile(project(":compiler:fir:checkers:checkers.jvm"))
    compile(project(":compiler:fir:java"))
    compile(project(":compiler:backend.common.jvm"))
    testCompile(project(":analysis:analysis-api-fir"))
    implementation(project(":compiler:ir.psi2ir"))
    implementation(project(":compiler:fir:entrypoint"))
    implementation(project(":analysis:analysis-api-providers"))

    compile(intellijCoreDep()) { includeJars("intellij-core", "guava", rootProject = rootProject) }


    testCompile(projectTests(":compiler:test-infrastructure-utils"))
    testCompile(projectTests(":compiler:test-infrastructure"))
    testCompile(projectTests(":compiler:tests-common-new"))

    testImplementation("org.opentest4j:opentest4j:1.2.0")
    testCompile(toolsJar())
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(projectTests(":compiler:fir:analysis-tests:legacy-fir-tests"))
    testCompile(project(":kotlin-test:kotlin-test-junit"))
    testApiJUnit5()
    testCompile(project(":kotlin-reflect"))
    testImplementation(project(":analysis:symbol-light-classes"))

    testRuntimeOnly(intellijDep()) {
        includeJars(
            "jps-model",
            "extensions",
            "util",
            "platform-api",
            "platform-impl",
            "idea",
            "guava",
            "trove4j",
            "asm-all",
            "log4j",
            "jdom",
            "streamex",
            "bootstrap",
            "jna",
            rootProject = rootProject
        )
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTest(jUnit5Enabled = true) {
    dependsOn(":dist")
    workingDir = rootDir
    useJUnitPlatform()
}

allprojects {
    tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>> {
        kotlinOptions {
            freeCompilerArgs += "-opt-in=org.jetbrains.kotlin.fir.symbols.SymbolInternals"
        }
    }
}

testsJar()

