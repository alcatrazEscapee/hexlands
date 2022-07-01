/*
 * Part of the HexLands mod.
 * Licensed under MIT. See the project LICENSE.txt for details.
 */

package com.alcatrazescapee.hexlands.world;

import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraftforge.common.world.ForgeWorldPreset;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraftforge.common.ForgeConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import com.alcatrazescapee.hexlands.HexLands;

@SuppressWarnings("unused")
public class HexLandsWorldType
{
    public static final DeferredRegister<ForgeWorldPreset> WORLD_TYPES = DeferredRegister.create(ForgeRegistries.Keys.WORLD_TYPES, HexLands.MOD_ID);

    public static final RegistryObject<ForgeWorldPreset> HEXLANDS = register("hexlands", HexWorldPreset.HEXLANDS);
    public static final RegistryObject<ForgeWorldPreset> HEXLANDS_OVERWORLD_ONLY = register("hexlands_overworld_only", HexWorldPreset.HEXLANDS_OVERWORLD_ONLY);

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void setDefault()
    {
        if (ForgeConfig.COMMON.defaultWorldType.get().equals("default"))
        {
            ((ForgeConfigSpec.ConfigValue) ForgeConfig.COMMON.defaultWorldType).set(HexLandsWorldType.HEXLANDS.getId().toString());
        }
    }

    private static RegistryObject<ForgeWorldPreset> register(String name, HexWorldPreset preset)
    {
        return WORLD_TYPES.register(name, () -> new ForgeWorldPreset(new Wrapper(preset)));
    }

    record Wrapper(HexWorldPreset preset) implements ForgeWorldPreset.IChunkGeneratorFactory
    {
        @Override
        public ChunkGenerator createChunkGenerator(RegistryAccess registryAccess, long seed, String generatorSettings)
        {
            return preset.createChunkGenerator(registryAccess, seed);
        }
        @Override
        public WorldGenSettings createSettings(RegistryAccess registryAccess, long seed, boolean generateStructures, boolean bonusChest, String generatorSettings)
        {
            return preset.createSettings(registryAccess, seed, generateStructures, bonusChest);
        }
    }
}
