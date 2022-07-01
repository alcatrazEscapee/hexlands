package com.alcatrazescapee.hexlands.mixin;

import java.util.List;
import net.minecraft.client.gui.screens.worldselection.WorldPreset;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(WorldPreset.class)
public interface WorldPresetAccessor
{
    @Accessor("PRESETS")
    static List<WorldPreset> accessor$PRESETS() { throw new AssertionError(); }
}
