package com.alcatrazescapee.hexlands.mixin;

import com.alcatrazescapee.hexlands.util.RandomStateAccessor;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.RandomState;
import org.spongepowered.asm.mixin.*;

@Mixin(RandomState.class)
public class RandomStateMixin implements RandomStateAccessor {
    @Shadow @Mutable
    private NoiseRouter router;

    @Shadow @Mutable
    private Climate.Sampler sampler;

    @Unique
    private boolean hexlands$requirePatching = true;

    @Override
    public final boolean hexlands$requirePatching() {
        return this.hexlands$requirePatching;
    }

    @Override
    public final void hexlands$set(NoiseRouter router, Climate.Sampler sampler) {
        this.router = router;
        this.sampler = sampler;
        this.hexlands$requirePatching = false;
    }
}
