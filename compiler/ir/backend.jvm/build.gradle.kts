plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(project(":compiler:backend"))
    compile(project(":compiler:ir.tree"))
    compile(project(":compiler:ir.backend.common"))
    api(project(":compiler:backend.common.jvm"))
    compileOnly(project(":compiler:ir.tree.impl"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core", "asm-all", rootProject = rootProject) }
}

sourceSets {
    "main" {
        projectDefault()
    }
    "test" {}
}
