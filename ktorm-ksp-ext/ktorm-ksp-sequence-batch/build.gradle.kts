plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":ktorm-ksp-codegen"))
    testImplementation(project(":ktorm-ksp-tests"))
}

configureMavenPublishing()
