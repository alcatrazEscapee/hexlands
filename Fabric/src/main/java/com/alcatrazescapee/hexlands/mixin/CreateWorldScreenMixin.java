package com.alcatrazescapee.hexlands.mixin;

import java.util.Optional;
import java.util.OptionalLong;
import java.util.Random;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldGenSettingsComponent;
import net.minecraft.client.gui.screens.worldselection.WorldPreset;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.alcatrazescapee.hexlands.FabricHexLandsClient;
import com.alcatrazescapee.hexlands.world.HexWorldPreset;

/**
 * Sets the hexlands world type as the default.
 */
@Mixin(CreateWorldScreen.class)
public abstract class CreateWorldScreenMixin
{
    @Dynamic("Targets the new WorldGenSettingsComponent(...)")
    @Redirect(method = "createFresh", at = @At(value = "NEW", target = "Lnet/minecraft/client/gui/screens/worldselection/WorldGenSettingsComponent;"), require = 0)
    private static WorldGenSettingsComponent useHexLandsAsDefault(RegistryAccess.Frozen registryAccess, WorldGenSettings settings, Optional<WorldPreset> preset, OptionalLong seed)
    {
        return preset.isPresent() && preset.get() == WorldPreset.NORMAL ?
            new WorldGenSettingsComponent(registryAccess, HexWorldPreset.HEXLANDS.createSettings(registryAccess, seed.orElse(new Random().nextLong()), true, false), Optional.of(FabricHexLandsClient.HEXLANDS), seed) :
            new WorldGenSettingsComponent(registryAccess, settings, preset, seed);
    }
}
