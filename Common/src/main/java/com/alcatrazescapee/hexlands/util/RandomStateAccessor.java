package com.alcatrazescapee.hexlands.util;

import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.RandomState;

public interface RandomStateAccessor
{
    static RandomStateAccessor of(RandomState state) {
        return (RandomStateAccessor) (Object) state;
    }

    boolean hexlands$requirePatching();

    void hexlands$set(NoiseRouter router, Climate.Sampler sampler);
}
