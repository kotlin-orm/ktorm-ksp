plugins {
    kotlin("jvm")
}

dependencies {
    api(kotlin("stdlib"))
    api(libs.ktorm.core)
    api(libs.cglib)
    testImplementation(libs.junit)
    testImplementation(libs.assertj.core)
}

configureMavenPublishing()
