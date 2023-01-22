package com.alcatrazescapee.hexlands.platform;

import java.util.ServiceLoader;

import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.RandomState;

public interface XPlatform
{
    XPlatform INSTANCE = find(XPlatform.class);

    static <T> T find(Class<T> clazz)
    {
        return ServiceLoader.load(clazz)
            .findFirst()
            .orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
    }

    boolean isNoiseDensityFunction(DensityFunction f);

    void patchSampler(Climate.Sampler hexSampler, RandomState state);
}
