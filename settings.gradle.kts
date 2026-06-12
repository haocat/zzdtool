pluginManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://api.xposed.info/")
        maven("https://www.jitpack.io")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://api.xposed.info/")
        maven("https://www.jitpack.io")
    }
}

rootProject.name = "zzdtool"
include(":app")
