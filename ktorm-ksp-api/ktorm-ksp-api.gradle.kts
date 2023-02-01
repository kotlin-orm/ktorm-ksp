
plugins {
    id("ktorm-ksp.module")
    id("ktorm-ksp.publish")
}

dependencies {
    api(libs.ktorm.core)
    testImplementation(libs.junit)
    testImplementation(libs.assertj.core)
}
