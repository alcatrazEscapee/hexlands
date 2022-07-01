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
    public static final WorldPresetInstance HEXLANDS = new WorldPresetInstance(HexWorldPreset.HEXLANDS, "hexlands");
    public static final WorldPresetInstance HEXLANDS_OVERWORLD_ONLY = new WorldPresetInstance(HexWorldPreset.HEXLANDS_OVERWORLD_ONLY, "hexlands_overworld_only");

    @Override
    public void onInitializeClient()
    {
        WorldPresetAccessor.accessor$PRESETS().add(HEXLANDS);
        WorldPresetAccessor.accessor$PRESETS().add(HEXLANDS_OVERWORLD_ONLY);
    }

    static class WorldPresetInstance extends WorldPreset
    {
        private final HexWorldPreset preset;

        WorldPresetInstance(HexWorldPreset preset, String name)
        {
            super(HexLands.MOD_ID + "." + name);
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
