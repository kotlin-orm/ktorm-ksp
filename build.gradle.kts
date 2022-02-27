plugins {
    kotlin("jvm") version "1.6.10" apply false
}

buildscript {
    dependencies {
        classpath(kotlin("gradle-plugin", version = "1.6.10"))
    }
}

allprojects {
    group = "org.ktorm"
    version = "0.0.1"

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions.jvmTarget = "1.8"
        kotlinOptions.allWarningsAsErrors = true
        kotlinOptions.freeCompilerArgs += "-Xexplicit-api=strict"
        kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    repositories {
        mavenCentral()
        jcenter()
    }

    dependencies {
    }
}