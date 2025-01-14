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

rootProject.name = "dagger-parent"

fun includeProject(name: String, path: String) {
    include(name)
    project(name).projectDir = File(path)
}

includeProject(":dagger", "gradle-projects/dagger-runtime")
includeProject(":dagger-spi", "gradle-projects/dagger-spi")
