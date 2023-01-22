package com.alcatrazescapee.hexlands;


import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.slf4j.Logger;

import com.alcatrazescapee.hexlands.world.HexChunkGenerator;

public final class HexLands
{
    public static final String MOD_ID = "hexlands";
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void init()
    {
        LOGGER.info("Wait, this isn't Catan...");
    }

    public static void registerCodecs()
    {
        Registry.register(BuiltInRegistries.CHUNK_GENERATOR, new ResourceLocation(MOD_ID, "hexlands"), HexChunkGenerator.CODEC);
    }
}