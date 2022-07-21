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

enableFeaturePreview("VERSION_CATALOGS")

include("ktorm-ksp-api")
include("ktorm-ksp-compiler")
include("ktorm-ksp-codegen")
include("ktorm-ksp-ext")
include("ktorm-ksp-ext:ktorm-ksp-sequence-batch")
include("ktorm-ksp-example")
include("ktorm-ksp-example:ktorm-ksp-example-common")
include("ktorm-ksp-example:ktorm-ksp-example-simple")
include("ktorm-ksp-example:ktorm-ksp-example-ext")
include("ktorm-ksp-tests")
