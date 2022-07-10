plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":ktorm-ksp-codegen"))
    implementation(libs.evoInflector)
    testImplementation(project(":ktorm-ksp-tests"))
}

configureMavenPublishing()
