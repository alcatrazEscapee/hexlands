plugins {
    java
    id("org.spongepowered.gradle.vanilla") version "0.2.1-SNAPSHOT"
}

// From gradle.properties
val minecraftVersion: String by extra

minecraft {
    accessWideners(project.file("src/main/resources/hexlands.common.accesswidener"))
    version(minecraftVersion)
}

dependencies {
    compileOnly(group = "org.spongepowered", name = "mixin", version = "0.8.5")
}
