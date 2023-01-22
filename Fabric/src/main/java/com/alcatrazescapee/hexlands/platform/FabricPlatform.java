package com.alcatrazescapee.hexlands.platform;

import net.fabricmc.fabric.impl.biome.MultiNoiseSamplerHooks;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.RandomState;

public final class FabricPlatform implements XPlatform
{
    @Override
    public boolean isNoiseDensityFunction(DensityFunction f)
    {
        return f instanceof DensityFunctions.Noise || f instanceof DensityFunctions.Shift || f instanceof DensityFunctions.ShiftedNoise;
    }

    @Override
    public void patchSampler(Climate.Sampler hexSampler, RandomState state) {
        try {
            ((MultiNoiseSamplerHooks) (Object) hexSampler).fabric_setSeed(((MultiNoiseSamplerHooks) (Object) state.sampler()).fabric_getSeed());
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
