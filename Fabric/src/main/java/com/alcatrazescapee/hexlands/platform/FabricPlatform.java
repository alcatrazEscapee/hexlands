package com.alcatrazescapee.hexlands.platform;

import net.fabricmc.fabric.impl.biome.MultiNoiseSamplerHooks;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;

public final class FabricPlatform implements XPlatform
{
    @Override
    public boolean isNoiseDensityFunction(DensityFunction f)
    {
        return f instanceof DensityFunctions.Noise || f instanceof DensityFunctions.Shift || f instanceof DensityFunctions.ShiftedNoise;
    }

    @Override
    @SuppressWarnings({"UnstableApiUsage", "ConstantConditions"})
    public void copyFabricCachedClimateSamplerSeed(Climate.Sampler from, Climate.Sampler to)
    {
        ((MultiNoiseSamplerHooks) (Object) to).fabric_setSeed(((MultiNoiseSamplerHooks) (Object) from).fabric_getSeed());
    }
}
