plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":ktorm-ksp-codegen"))
    implementation(libs.evoInflector)
    testImplementation(libs.junit)
    testImplementation(libs.assertj.core)
    testImplementation(libs.kotlinCompileTesting)
    testImplementation(libs.kotlinCompileTesting.ksp)
    testImplementation(libs.h2database)
}

configureMavenPublishing()
