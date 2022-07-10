plugins {
    kotlin("jvm")
    `java-gradle-plugin`
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
            implementationClass = "org.ktorm.ksp.enhance.gradle.KtormKspEnhanceGradlePlugin"
            version = project.version
        }
    }
}