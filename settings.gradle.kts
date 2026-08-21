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
        maven { url = uri("https://jitpack.io") }
        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
            credentials {
                val properties = java.util.Properties()
                val localPropertiesFile = java.io.File(rootDir, "local.properties")
                if (localPropertiesFile.exists()) {
                    properties.load(localPropertiesFile.inputStream())
                }
                username = "mapbox"
                password = properties.getProperty("MAPBOX_SECRET_TOKEN") ?: System.getenv("MAPBOX_SECRET_TOKEN") ?: ""
            }
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
}

rootProject.name = "PocketPlanner"
include(":app")
