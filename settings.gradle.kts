pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven { url = uri("https://repo.eclipse.org/content/repositories/paho-releases/") }
        maven { url = uri("https://jitpack.io") }  // Added JitPack for MQTT library
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://repo.eclipse.org/content/repositories/paho-releases/") }
        maven { url = uri("https://jitpack.io") }  // Added JitPack for MQTT library
    }
}

rootProject.name = "IOT"
include(":app")
