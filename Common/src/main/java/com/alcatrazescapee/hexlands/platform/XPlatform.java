package com.alcatrazescapee.hexlands.platform;

import java.util.ServiceLoader;

import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.DensityFunction;

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

    /** Fabric does a stupid thing and sets a cached seed on `Climate.Sampler`, and then blows up if it's not there... how annoying */
    default void copyFabricCachedClimateSamplerSeed(Climate.Sampler from, Climate.Sampler to) {}
}
