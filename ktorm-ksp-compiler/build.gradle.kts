plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":ktorm-ksp-spi"))
    implementation(libs.evoInflector)
    implementation(libs.ktfmt)
    testImplementation(project(":ktorm-ksp-tests"))
}

configureMavenPublishing()
