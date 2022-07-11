plugins {
    kotlin("jvm")
    `java-gradle-plugin`
    id("com.github.gmazzo.buildconfig")
    id("com.gradle.plugin-publish")
}

buildConfig {
    buildConfigField("String", "PLUGIN_VERSION", provider { "\"${project.version}\"" })
}

dependencies {
    compileOnly(libs.kotlin.compiler.embeddable)
    implementation(project(":ktorm-ksp-codegen"))
    implementation(libs.kotlin.gradle.plugin.api)
    testImplementation(project(":ktorm-ksp-tests"))
}

configureMavenPublishing()

gradlePlugin {
    plugins {
        create("ktorm-ksp-enhance") {
            id = "org.ktorm.ksp.enhance"
            displayName = "ktorm-ksp-enhance"
            description = "Enhance ktorm-ksp with kotlin compiler plugin for more functionality"
            implementationClass = "org.ktorm.ksp.enhance.gradle.KtormKspEnhanceGradlePlugin"
            version = project.version
        }
    }
}

pluginBundle {
    website = "https://github.com/kotlin-orm/ktorm-ksp"
    vcsUrl = "https://github.com/kotlin-orm/ktorm-ksp"
    tags = listOf("kotlin", "ktorm", "ktorm-ksp", "kotlin-compiler-plugin")
}