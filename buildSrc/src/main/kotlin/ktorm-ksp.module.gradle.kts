
group = rootProject.group
version = rootProject.version

plugins {
    id("kotlin")
    id("org.gradle.jacoco")
    id("io.gitlab.arturbosch.detekt")
    id("ktorm-ksp.source-header-check")
}

repositories {
    mavenCentral()
}

dependencies {
    api(kotlin("stdlib"))
    api(kotlin("reflect"))
    testImplementation(kotlin("test-junit"))
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:${detekt.toolVersion}")
}

detekt {
    source = files("src/main/kotlin")
    config = files("${project.rootDir}/detekt.yml")
}

tasks {
    compileKotlin {
        kotlinOptions {
            jvmTarget = "1.8"
            allWarningsAsErrors = true

            if (project.name == "ktorm-ksp-api" || project.name == "ktorm-ksp-spi") {
                freeCompilerArgs = listOf("-Xexplicit-api=strict")
            }
        }
    }

    jacocoTestReport {
        reports {
            csv.required.set(true)
            xml.required.set(true)
            html.required.set(true)
        }
    }
}
