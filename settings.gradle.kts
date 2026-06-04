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
        // Kakao SDK(v2-share 등)는 Maven Central 미게시 → Kakao 전용 repo 필요.
        maven { url = uri("https://devrepo.kakao.com/nexus/content/groups/public/") }
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
