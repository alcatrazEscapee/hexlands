pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()

        fun exclusiveMaven(url: String, filter: Action<InclusiveRepositoryContentDescriptor>) =
            exclusiveContent {
                forRepository { maven(url) }
                filter(filter)
            }

        exclusiveMaven("https://maven.fabricmc.net") {
            includeGroup("net.fabricmc")
            includeGroup("fabric-loom")
        }
        exclusiveMaven("https://maven.neoforged.net/releases") {
            includeGroupAndSubgroups("net.neoforged")
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "HexLands-1.21"
include("Common", "Fabric", "NeoForge")