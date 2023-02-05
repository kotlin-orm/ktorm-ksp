
plugins {
    id("ktorm-ksp.module")
    id("ktorm-ksp.publish")
    id("ktorm-ksp.source-header-check")
}

dependencies {
    compileOnly(libs.ktorm.core)
    testImplementation(libs.ktorm.core)
}
