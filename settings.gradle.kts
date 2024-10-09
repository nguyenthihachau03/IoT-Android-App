pluginManagement {
    repositories {
        maven {
            setUrl("https://repo.eclipse.org/content/repositories/paho-releases/")
        }
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

dependencyResolutionManagement {
    repositories {
        maven {
            setUrl("https://repo.eclipse.org/content/repositories/paho-releases/")
        }
        mavenCentral()
        google()
    }
}

rootProject.name = "IOT"
include(":app")
 