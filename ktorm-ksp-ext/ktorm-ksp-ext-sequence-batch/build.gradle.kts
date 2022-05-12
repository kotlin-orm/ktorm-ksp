plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":ktorm-ksp-codegen"))
}

configureMavenPublishing()
