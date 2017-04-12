allprojects {

    group = "org.gradle.script.kotlin.samples.multiproject"

    version = "1.0"

    repositories {
        gradleScriptKotlin()
    }
}

plugins {
    base
}

dependencies {
    // Make the root project archives configuration depend on every sub-project
    subprojects.forEach {
        archives(it)
    }
}
