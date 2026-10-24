// 统一依赖版本
pluginManagement {
    repositories {
        google()
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

rootProject.name = "MofangVideo"

    include(":app")
    include(":app-tv")
    include(":core")
include(":feature-video")
include(":feature-live")
include(":feature-backup")
include(":feature-settings")
include(":feature-history")
