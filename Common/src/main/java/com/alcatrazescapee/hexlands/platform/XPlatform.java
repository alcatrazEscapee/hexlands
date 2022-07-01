package com.alcatrazescapee.hexlands.platform;

import java.util.ServiceLoader;

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
}
