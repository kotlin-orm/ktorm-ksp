plugins {
    id("com.google.devtools.ksp")
    kotlin("jvm")
}

dependencies {
    ksp(project(":ktorm-ksp-compiler"))
    implementation(project(":ktorm-ksp-api"))
    implementation(project(":ktorm-ksp-example:ktorm-ksp-example-common"))
}

kotlin {
    sourceSets.main {
        kotlin.srcDir("build/generated/ksp/main/kotlin")
    }
    sourceSets.test {
        kotlin.srcDir("build/generated/ksp/test/kotlin")
    }
}
