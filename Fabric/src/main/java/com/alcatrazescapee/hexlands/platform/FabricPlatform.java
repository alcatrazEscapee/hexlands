package com.alcatrazescapee.hexlands.platform;

import net.minecraft.world.level.levelgen.DensityFunction;

public final class FabricPlatform implements XPlatform
{
    @Override
    public boolean isNoiseDensityFunction(DensityFunction f)
    {
        return false;
    }
}
