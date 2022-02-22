plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":ktorm-ksp-annotations"))
    api("org.ktorm:ktorm-core:3.4.1")
    api("com.google.devtools.ksp:symbol-processing-api:1.6.10-1.0.2")
    api("com.squareup:kotlinpoet-ksp:1.10.2")
}