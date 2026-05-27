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

rootProject.name = "BarNote"

include(":app")
include(":core:domain")
include(":core:data")
include(":core:network")
include(":core:designsystem")
include(":core:oqcore")
include(":feature:home")
include(":feature:mypage")
include(":feature:search")
include(":feature:settings")
