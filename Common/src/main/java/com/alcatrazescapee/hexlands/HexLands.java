package com.alcatrazescapee.hexlands;


import com.mojang.logging.LogUtils;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
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