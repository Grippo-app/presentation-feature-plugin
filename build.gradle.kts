plugins {
    kotlin("jvm") version "1.9.23"
    id("org.jetbrains.intellij") version "1.17.3"
}

group = "feature.plugin"
version = "1.0"

repositories {
    mavenCentral()
}

intellij {
    type.set("AI")
    version.set("2024.1.1.3")
    plugins.set(listOf())
}

tasks {
    patchPluginXml {
        sinceBuild.set("241") // Meerkat = 2024.1 → build 241.*
        untilBuild.set("999.*")
    }

    compileKotlin {
        kotlinOptions.jvmTarget = "19"
    }

    runIde {
        ideDir.set(file("/Applications/Android Studio.app/Contents"))
    }
}