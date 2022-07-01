package com.alcatrazescapee.hexlands.world;

import net.minecraft.core.QuartPos;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseRouterData;
import net.minecraft.world.level.levelgen.NoiseSettings;

public abstract class NoiseRouterAccessor extends NoiseRouterData
{
    public static double computePreliminarySurfaceLevelScanning(NoiseSettings settings, DensityFunction preliminarySurfaceNoJaggedness, int quartX, int quartZ)
    {
        return NoiseRouterData.computePreliminarySurfaceLevelScanning(settings, preliminarySurfaceNoJaggedness, QuartPos.toBlock(quartX), QuartPos.toBlock(quartZ));
    }
}
