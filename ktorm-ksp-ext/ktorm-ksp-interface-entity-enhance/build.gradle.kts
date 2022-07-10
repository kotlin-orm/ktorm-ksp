plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":ktorm-ksp-codegen"))
    testImplementation(project(":ktorm-ksp-enhance"))
    testImplementation(project(":ktorm-ksp-tests"))
}

configureMavenPublishing()
