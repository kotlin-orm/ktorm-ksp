
plugins {
    id("ktorm-ksp.module")
    id("ktorm-ksp.publish")
}

dependencies {
    api(kotlin("stdlib"))
    api(project(":ktorm-ksp-api"))
    api(libs.ktorm.core)
    api(libs.ksp.api)
    api(libs.kotlinpoet.ksp)
}
