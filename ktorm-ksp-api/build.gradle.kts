plugins {
    kotlin("jvm")
}

dependencies {
    api(kotlin("stdlib"))
    api(libs.ktorm.core)
    implementation(libs.bytebuddy)
    testImplementation(libs.junit)
    testImplementation(libs.assertj.core)
}

configureMavenPublishing()

