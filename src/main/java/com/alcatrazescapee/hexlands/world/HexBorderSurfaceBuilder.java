/*
 * Part of the HexLands mod.
 * Licensed under MIT. See the project LICENSE.txt for details.
 */

package com.alcatrazescapee.hexlands.world;

import java.util.Random;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.surfacebuilders.SurfaceBuilder;
import net.minecraft.world.gen.surfacebuilders.SurfaceBuilderConfig;

import com.mojang.serialization.Codec;

/**
 * Not registered. Just used to build simple full down-to-bedrock border walls if it is desired.
 */
public class HexBorderSurfaceBuilder extends SurfaceBuilder<SurfaceBuilderConfig>
{
    private final BlockState borderState;

    public HexBorderSurfaceBuilder(BlockState borderState)
    {
        super(Codec.unit(SurfaceBuilder.CONFIG_STONE));

        this.borderState = borderState;
    }

    @Override
    public void apply(Random random, IChunk chunkIn, Biome biomeIn, int x, int z, int startHeight, double noise, BlockState defaultBlock, BlockState defaultFluid, int seaLevel, long seed, SurfaceBuilderConfig config)
    {
        final BlockPos.Mutable pos = new BlockPos.Mutable();

        pos.set(x & 15, startHeight, z & 15);
        for (int y = startHeight; y >= 0; --y)
        {
            pos.setY(y);

            final BlockState state = chunkIn.getBlockState(pos);
            if (state.is(defaultBlock.getBlock()))
            {
                chunkIn.setBlockState(pos, borderState, false);
            }
        }
    }
}
