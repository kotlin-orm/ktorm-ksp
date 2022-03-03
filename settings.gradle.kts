rootProject.name = "ktorm-ksp"

include("ktorm-ksp-api")
include("ktorm-ksp-compiler")
include("ktorm-ksp-demo")
include("ktorm-ksp-codegen")
include("ktorm-ksp-ext")
include("ktorm-ksp-ext:ktorm-ksp-ext-sequence-batch")

pluginManagement {
    val kotlinVersion: String by settings
    val googleKspVersion: String by settings
    resolutionStrategy {
        eachPlugin {
            when (requested.id.id) {
                "org.jetbrains.kotlin.jvm" -> useVersion(kotlinVersion)
                "com.google.devtools.ksp" -> useVersion(googleKspVersion)
            }
        }
    }
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            val ktormVersion: String by settings
            val kotlinpoetKspVersion: String by settings
            val googleKspVersion:String by settings
            library("ktorm-core", "org.ktorm:ktorm-core:$ktormVersion")
            library("kotlinpoet-ksp","com.squareup:kotlinpoet-ksp:$kotlinpoetKspVersion")
            library("ksp-api","com.google.devtools.ksp:symbol-processing-api:$googleKspVersion")
        }
    }
}