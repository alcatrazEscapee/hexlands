plugins {
    id("net.neoforged.moddev") version "0.1.112"
}

val parchmentMinecraftVersion: String by extra
val parchmentVersion: String by extra
val commonNeoFormVersion: String by extra

dependencies {
    compileOnly(group = "org.spongepowered", name = "mixin", version = "0.8.5")
}

neoForge {
    neoFormVersion.set(commonNeoFormVersion)

    parchment {
        minecraftVersion.set(parchmentMinecraftVersion)
        mappingsVersion.set(parchmentVersion)
    }
}
