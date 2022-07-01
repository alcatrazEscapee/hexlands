package com.alcatrazescapee.hexlands;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.worldselection.WorldPreset;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.WorldGenSettings;

import com.alcatrazescapee.hexlands.mixin.WorldPresetAccessor;
import com.alcatrazescapee.hexlands.world.HexWorldPreset;

public final class FabricHexLandsClient implements ClientModInitializer
{
    @Override
    public void onInitializeClient()
    {
        WorldPresetAccessor.accessor$PRESETS().add(new WorldPresetInstance(HexWorldPreset.HEXLANDS, "hexlands"));
        WorldPresetAccessor.accessor$PRESETS().add(new WorldPresetInstance(HexWorldPreset.HEXLANDS_OVERWORLD_ONLY, "hexlands_overworld_only"));
    }

    static class WorldPresetInstance extends WorldPreset
    {
        private final HexWorldPreset preset;

        WorldPresetInstance(HexWorldPreset preset, String name)
        {
            super(name);
            this.preset = preset;
        }

        @Override
        public WorldGenSettings create(RegistryAccess registryAccess, long seed, boolean generateFeatures, boolean generateBonusChest)
        {
            return preset.createSettings(registryAccess, seed, generateFeatures, generateBonusChest);
        }

        @Override
        protected ChunkGenerator generator(RegistryAccess registryAccess, long seed)
        {
            return preset.createChunkGenerator(registryAccess, seed);
        }
    }
}
