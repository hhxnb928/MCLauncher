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
        maven("https://pojav.net/landing/maven/")
        maven("https://api.modrinth.com/maven")
    }
}
rootProject.name = "MCLauncher"
include(":app")
