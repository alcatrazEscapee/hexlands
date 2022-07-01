package com.alcatrazescapee.hexlands.mixin;

import java.util.List;
import net.minecraft.client.gui.screens.worldselection.WorldPreset;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Used to add new world presets to the create world screen.
 */
@Mixin(WorldPreset.class)
public interface WorldPresetAccessor
{
    @Accessor("PRESETS")
    static List<WorldPreset> accessor$PRESETS() { throw new AssertionError(); }
}
