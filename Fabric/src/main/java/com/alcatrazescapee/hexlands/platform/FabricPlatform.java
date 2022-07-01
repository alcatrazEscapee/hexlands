package com.alcatrazescapee.hexlands.platform;

import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;

public final class FabricPlatform implements XPlatform
{
    @Override
    public boolean isNoiseDensityFunction(DensityFunction f)
    {
        return f instanceof DensityFunctions.Noise || f instanceof DensityFunctions.Shift || f instanceof DensityFunctions.ShiftedNoise;
    }
}
