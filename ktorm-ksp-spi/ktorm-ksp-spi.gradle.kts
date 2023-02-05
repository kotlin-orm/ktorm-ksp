
plugins {
    id("ktorm-ksp.module")
    id("ktorm-ksp.publish")
    id("ktorm-ksp.source-header-check")
}

dependencies {
    api(kotlin("stdlib"))
    api(project(":ktorm-ksp-api"))
    api(libs.ktorm.core)
    api(libs.ksp.api)
    api(libs.kotlinpoet.ksp)
}
