
plugins {
    id("ktorm-ksp.module")
    id("ktorm-ksp.publish")
    id("ktorm-ksp.source-header-check")
}

dependencies {
    api(project(":ktorm-ksp-spi"))
    implementation(libs.evoInflector)
    implementation(libs.ktfmt)
    testImplementation(project(":ktorm-ksp-compiler"))
    testImplementation(libs.junit)
    testImplementation(libs.assertj.core)
    testImplementation(libs.kotlinCompileTesting)
    testImplementation(libs.kotlinCompileTesting.ksp)
    testImplementation(libs.h2database)
}
