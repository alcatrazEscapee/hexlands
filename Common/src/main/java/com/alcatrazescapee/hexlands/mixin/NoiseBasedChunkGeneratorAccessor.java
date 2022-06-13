package com.alcatrazescapee.hexlands.mixin;

import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseRouter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(NoiseBasedChunkGenerator.class)
public interface NoiseBasedChunkGeneratorAccessor
{
    @Mutable
    @Accessor("router")
    void setRouter(NoiseRouter router);

    @Mutable
    @Accessor
    void setSampler(Climate.Sampler sampler);
}
