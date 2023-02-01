
plugins {
    id("com.gradle.enterprise") version("3.9")
}

include("ktorm-ksp-api")
include("ktorm-ksp-compiler")
include("ktorm-ksp-example")
include("ktorm-ksp-spi")

rootProject.name = "ktorm-ksp"
rootProject.children.forEach { project ->
    project.buildFileName = "${project.name}.gradle.kts"
}

gradleEnterprise {
    if (System.getenv("CI") == "true") {
        buildScan {
            publishAlways()
            termsOfServiceUrl = "https://gradle.com/terms-of-service"
            termsOfServiceAgree = "yes"
        }
    }
}
