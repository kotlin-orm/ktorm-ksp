plugins {
    kotlin("jvm")
}

dependencies {
    api(kotlin("stdlib"))
    api(libs.ktorm.core)
    testImplementation(libs.junit)
    testImplementation(libs.assertj.core)
}

