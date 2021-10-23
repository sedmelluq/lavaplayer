include("common")
include("main")
include("testbot")
include(":extensions:ip-rotator")

pluginManagement {
    resolutionStrategy {
        repositories {
            gradlePluginPortal()
        }

        eachPlugin {
            if (requested.id.id == "kotlinx-atomicfu") {
                useModule("org.jetbrains.kotlinx:atomicfu-gradle-plugin:${requested.version}")
            }
        }
    }
}
