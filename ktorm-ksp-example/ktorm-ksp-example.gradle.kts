plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp") version "1.7.22-1.0.8"
}

dependencies {
    api(libs.ktorm.core)
    api(libs.h2database)
    ksp(project(":ktorm-ksp-compiler"))
    implementation(project(":ktorm-ksp-api"))
}

kotlin {
    sourceSets.main {
        kotlin.srcDir("build/generated/ksp/main/kotlin")
    }
    sourceSets.test {
        kotlin.srcDir("build/generated/ksp/test/kotlin")
    }
}
