plugins {
    id("com.google.devtools.ksp")
    kotlin("jvm")
}


dependencies {
    ksp(project(":ktorm-ksp-compiler"))
    ksp(project(":ktorm-ksp-ext:ktorm-ksp-ext-sequence-batch"))
    implementation(project(":ktorm-ksp-api"))
    implementation(libs.ktorm.core)
}

kotlin {
    sourceSets.main {
        kotlin.srcDir("build/generated/ksp/main/kotlin")
    }
    sourceSets.test {
        kotlin.srcDir("build/generated/ksp/test/kotlin")
    }
}
