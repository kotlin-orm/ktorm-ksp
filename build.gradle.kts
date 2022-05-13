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

    val detekt by configurations.creating
    val detektVersion:String by project

    tasks.register<JavaExec>("detekt") {
        mainClass.set("io.gitlab.arturbosch.detekt.cli.Main")
        classpath = detekt
        val input = "${projectDir}/src/main/kotlin"
        val config = "${rootDir}/detekt.yml"
        val params = listOf("-i", input, "-c", config)
        args(params)
    }

    dependencies {
        detekt("io.gitlab.arturbosch.detekt:detekt-cli:${detektVersion}")
        detekt("io.gitlab.arturbosch.detekt:detekt-formatting:${detektVersion}")
    }

    repositories {
        mavenCentral()
        jcenter()
    }
}
