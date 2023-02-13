
plugins {
    id("ktorm-ksp.module")
    id("ktorm-ksp.publish")
    id("ktorm-ksp.source-header-check")
}

dependencies {
    compileOnly(libs.ktorm.core)
    compileOnly(libs.ktorm.jackson)
    testImplementation(libs.ktorm.core)
}
