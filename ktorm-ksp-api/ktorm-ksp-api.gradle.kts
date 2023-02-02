
plugins {
    id("ktorm-ksp.module")
    id("ktorm-ksp.publish")
}

dependencies {
    compileOnly(libs.ktorm.core)
    testImplementation(libs.ktorm.core)
}
