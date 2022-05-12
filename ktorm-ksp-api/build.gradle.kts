plugins {
    kotlin("jvm")
}

dependencies {
    api(kotlin("stdlib"))
    compileOnly(libs.ktorm.core)
}

configureMavenPublishing()
