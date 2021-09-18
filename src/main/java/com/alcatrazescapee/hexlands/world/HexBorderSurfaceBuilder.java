/*
 * Part of the HexLands mod.
 * Licensed under MIT. See the project LICENSE.txt for details.
 */

package com.alcatrazescapee.hexlands.world;

import java.util.Random;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.surfacebuilders.SurfaceBuilder;
import net.minecraft.world.level.levelgen.surfacebuilders.SurfaceBuilderBaseConfiguration;

import com.mojang.serialization.Codec;

/**
 * Not registered. Just used to build simple full down-to-bedrock border walls if it is desired.
 */
public class HexBorderSurfaceBuilder extends SurfaceBuilder<SurfaceBuilderBaseConfiguration>
{
    private final BlockState borderState;

    public HexBorderSurfaceBuilder(BlockState borderState)
    {
        super(Codec.unit(SurfaceBuilder.CONFIG_STONE));

        this.borderState = borderState;
    }

    @Override
    public void apply(Random random, ChunkAccess chunk, Biome biome, int x, int z, int startHeight, double noise, BlockState defaultBlock, BlockState defaultFluid, int seaLevel, int minSurfaceLevel, long seed, SurfaceBuilderBaseConfiguration config)
    {
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        pos.set(x & 15, startHeight, z & 15);
        for (int y = startHeight; y >= 0; --y)
        {
            pos.setY(y);

            final BlockState state = chunk.getBlockState(pos);
            if (state.is(defaultBlock.getBlock()))
            {
                chunk.setBlockState(pos, borderState, false);
            }
        }
    }
}
