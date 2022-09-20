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
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.register
import java.net.URI

fun Project.configureMavenPublishing() {
    apply {
        plugin("signing")
        plugin("maven-publish")
    }

    val generateSources = tasks.register<Jar>("generateSources") {
        archiveClassifier.set("sources")
        from(sourceSets["main"].allSource)
    }
    val generateJavadoc = tasks.register<Jar>("generateJavadoc") {
        archiveClassifier.set("javadoc")
    }

    publishing {
        publications {
            create<MavenPublication>("dist") {
                from(components["java"])
                artifact(generateSources)
                artifact(generateJavadoc)
                group = project.group
                artifactId = project.name
                version = project.version.toString()
                pom {
                    name.set("${project.group}:${project.name}")
                    description.set("Ktorm KSP extension to help generate boilerplate code.")
                    url.set("https://www.ktorm.org")
                    licenses {
                        license {
                            name.set("The Apache Software License, Version 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                    scm {
                        url.set("https://github.com/kotlin-orm/ktorm-ksp")
                        connection.set("scm:git:https://github.com/kotlin-orm/ktorm-ksp.git")
                        developerConnection.set("scm:git:ssh://git@github.com/kotlin-orm/ktorm-ksp.git")
                    }
                    developers {
                        developer {
                            id.set("lookup-cat")
                            name.set("夜里的向日葵")
                            email.set("641571835@qq.com")
                        }
                    }
                }
                repositories {
                    maven {
                        name = "central"
                        url = URI("https://oss.sonatype.org/service/local/staging/deploy/maven2")
                        credentials {
                            username = System.getenv("OSSRH_USER")
                            password = System.getenv("OSSRH_PASSWORD")
                        }
                    }
                    maven {
                        name = "snapshot"
                        url = URI("https://oss.sonatype.org/content/repositories/snapshots")
                        credentials {
                            username = System.getenv("OSSRH_USER")
                            password = System.getenv("OSSRH_PASSWORD")
                        }
                    }
                }
            }
        }
    }
    signing {
        val keyId = System.getenv("GPG_KEY_ID")
        val secretKey = System.getenv("GPG_SECRET_KEY")
        val password = System.getenv("GPG_PASSWORD")

        assert(!project.version.toString().endsWith("SNAPSHOT"))
        useInMemoryPgpKeys(keyId, secretKey, password)
        sign(publications["dist"])
    }
}
