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

rootProject.name = "GearsApp"

// :core contiene todos los cálculos en Kotlin puro (sin Android) para poder probarlos fácilmente.
include(":core")
// :app es la aplicación Android (pantallas, guardado de máquinas, etc.).
// Se puede omitir con -PsoloCore=true en equipos sin el SDK de Android.
if (!providers.gradleProperty("soloCore").isPresent) {
    include(":app")
}
