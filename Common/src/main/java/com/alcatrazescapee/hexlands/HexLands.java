package com.alcatrazescapee.hexlands;


import java.util.function.BiConsumer;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.slf4j.Logger;

import com.alcatrazescapee.hexlands.world.HexChunkGenerator;

public final class HexLands
{
    public static final String MOD_ID = "hexlands";
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void init(BiConsumer<ResourceLocation, MapCodec<? extends ChunkGenerator>> registry)
    {
        LOGGER.info("Wait, this isn't Catan...");
        registry.accept(ResourceLocation.fromNamespaceAndPath(MOD_ID, "hexlands"), HexChunkGenerator.CODEC);
    }
}