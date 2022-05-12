plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    jcenter()
}

kotlin {
    sourceSets.all {
        languageSettings.optIn("kotlin.Experimental")
        languageSettings.optIn("kotlin.RequiresOptIn")
    }
}

dependencies {
    api(gradleApi())
}
