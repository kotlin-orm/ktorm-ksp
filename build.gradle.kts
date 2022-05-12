plugins {
    kotlin("jvm") apply false
}

buildscript {
    dependencies {
        val gradlePluginVersion: String by project
        classpath(kotlin("gradle-plugin", version = gradlePluginVersion))
    }
}

allprojects {
    group = "org.ktorm"
    version = "1.0"

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions.jvmTarget = "1.8"
        kotlinOptions.allWarningsAsErrors = true
        kotlinOptions.freeCompilerArgs += "-Xexplicit-api=strict"
        kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    repositories {
        mavenCentral()
        jcenter()
    }

}
