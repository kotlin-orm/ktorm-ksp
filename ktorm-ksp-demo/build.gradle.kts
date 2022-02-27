plugins {
    id("com.google.devtools.ksp") version "1.6.10-1.0.4"
    kotlin("jvm")
}


dependencies {
    ksp(project(":ktorm-ksp-compiler"))
    implementation(project(":ktorm-ksp-api"))
    implementation("org.ktorm:ktorm-core:3.4.1")
}

kotlin {
    sourceSets.main {
        kotlin.srcDir("build/generated/ksp/main/kotlin")
    }
    sourceSets.test {
        kotlin.srcDir("build/generated/ksp/test/kotlin")
    }
}
