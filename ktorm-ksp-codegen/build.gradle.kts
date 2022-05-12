plugins {
    kotlin("jvm")
}

dependencies {
    api(kotlin("stdlib"))
    api(project(":ktorm-ksp-api"))
    api(libs.ktorm.core)
    api(libs.ksp.api)
    api(libs.kotlinpoet.ksp)
}

configureMavenPublishing()
