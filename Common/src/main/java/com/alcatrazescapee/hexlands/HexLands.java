package com.alcatrazescapee.hexlands;


import com.mojang.logging.LogUtils;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import com.alcatrazescapee.hexlands.world.HexChunkGenerator;
import com.alcatrazescapee.hexlands.world.HexEndBiomeSource;

public final class HexLands
{
    public static final String MOD_ID = "hexlands";
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void init()
    {
        LOGGER.info("Wait, this isn't Catan...");

        Config.INSTANCE.setup();
    }

    public static void registerCodecs()
    {
        Registry.register(Registry.CHUNK_GENERATOR, new ResourceLocation(MOD_ID, "hexlands"), HexChunkGenerator.CODEC);
        Registry.register(Registry.BIOME_SOURCE, new ResourceLocation(MOD_ID, "the_end"), HexEndBiomeSource.CODEC);
    }
}