/*
 * Copyright 2018-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

rootProject.name = "ktorm-ksp"

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
            val googleKspVersion: String by settings
            val h2databaseVersion: String by settings
            val kotlinCompileTestingVersion: String by settings
            val junitVersion: String by settings
            val assertjVersion: String by settings
            val evoInflectorVersion: String by settings
            library("ktorm-core", "org.ktorm:ktorm-core:$ktormVersion")
            library("kotlinpoet-ksp", "com.squareup:kotlinpoet-ksp:$kotlinpoetKspVersion")
            library("ksp-api", "com.google.devtools.ksp:symbol-processing-api:$googleKspVersion")
            library("h2database", "com.h2database:h2:$h2databaseVersion")
            library(
                "kotlinCompileTesting",
                "com.github.tschuchortdev:kotlin-compile-testing:${kotlinCompileTestingVersion}"
            )
            library(
                "kotlinCompileTesting-ksp",
                "com.github.tschuchortdev:kotlin-compile-testing-ksp:${kotlinCompileTestingVersion}"
            )
            library("junit", "junit:junit:$junitVersion")
            library("assertj-core", "org.assertj:assertj-core:${assertjVersion}")
            library("evoInflector", "org.atteo:evo-inflector:${evoInflectorVersion}")
        }
    }
}

include("ktorm-ksp-api")
include("ktorm-ksp-compiler")
include("ktorm-ksp-codegen")
include("ktorm-ksp-ext")
include("ktorm-ksp-ext:ktorm-ksp-ext-sequence-batch")
include("ktorm-ksp-example")
include("ktorm-ksp-example:ktorm-ksp-example-common")
include("ktorm-ksp-example:ktorm-ksp-example-simple")
include("ktorm-ksp-example:ktorm-ksp-example-ext")
