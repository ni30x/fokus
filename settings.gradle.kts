@file:Suppress("UnstableApiUsage")


// Verify JDK configuration non-fatally
run {
    val jlink = File(System.getProperty("java.home"), "bin/jlink")
    if (!(jlink.isFile && jlink.canExecute())) {
        logger.warn(
            "Gradle JVM may be missing jlink at ${jlink.absolutePath}. " +
            "Android builds require a full JDK (Android Studio JBR, Temurin 21, etc.)."
        )
    }
}


pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "fokus"
include(":app")
 