plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":ktorm-ksp-spi"))
    api(project(":ktorm-ksp-compiler"))
    api(libs.junit)
    api(libs.assertj.core)
    api(libs.kotlinCompileTesting)
    api(libs.kotlinCompileTesting.ksp)
    api(libs.h2database)
}

