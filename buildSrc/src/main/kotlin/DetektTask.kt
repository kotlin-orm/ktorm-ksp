/*
 * Copyright 2022 the original author or authors.
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

import org.gradle.api.Project
import org.gradle.api.tasks.JavaExec
import org.gradle.kotlin.dsl.*

fun Project.configureDetekt() {
    val detekt by configurations.creating
    val detektVersion: String by project

    dependencies {
        detekt("io.gitlab.arturbosch.detekt:detekt-cli:${detektVersion}")
        detekt("io.gitlab.arturbosch.detekt:detekt-formatting:${detektVersion}")
    }

    tasks.register<JavaExec>("detekt") {
        group = "verification"
        mainClass.set("io.gitlab.arturbosch.detekt.cli.Main")
        classpath = detekt
        val input = "${projectDir}/src/main/kotlin"
        val config = "${rootDir}/detekt.yml"
        val params = listOf("-i", input, "-c", config)
        args(params)
    }
}
