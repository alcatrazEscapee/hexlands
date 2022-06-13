/*
 * Part of the HexLands mod.
 * Licensed under MIT. See the project LICENSE.txt for details.
 */

package com.alcatrazescapee.hexlands.world;

import java.util.function.Supplier;

import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraftforge.common.world.ForgeWorldPreset;
import net.minecraftforge.registries.RegistryObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraftforge.common.ForgeConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import com.alcatrazescapee.hexlands.HexLands;
import com.alcatrazescapee.hexlands.util.HexSettings;

@SuppressWarnings("unused")
public class HexLandsWorldType
{
    public static final DeferredRegister<ForgeWorldPreset> WORLD_TYPES = DeferredRegister.create(ForgeRegistries.Keys.WORLD_TYPES, HexLands.MOD_ID);

    public static final RegistryObject<ForgeWorldPreset> HEX_LANDS = register("hexlands", () -> new Impl(false));
    public static final RegistryObject<ForgeWorldPreset> HEX_LANDS_OVERWORLD_ONLY = register("hexlands_overworld_only", () -> new Impl(true));

    private static final Logger LOGGER = LogManager.getLogger();

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void setDefault()
    {
        if (ForgeConfig.COMMON.defaultWorldType.get().equals("default"))
        {
            ((ForgeConfigSpec.ConfigValue) ForgeConfig.COMMON.defaultWorldType).set(HexLandsWorldType.HEX_LANDS.getId().toString());
        }
    }

    private static RegistryObject<ForgeWorldPreset> register(String name, Supplier<ForgeWorldPreset.IChunkGeneratorFactory> factory)
    {
        return WORLD_TYPES.register(name, () -> new ForgeWorldPreset(factory.get()));
    }

    static class Impl implements ForgeWorldPreset.IBasicChunkGeneratorFactory
    {
        private final boolean overworldOnly;

        Impl(boolean overworldOnly)
        {
            this.overworldOnly = overworldOnly;
        }

        @Override
        public ChunkGenerator createChunkGenerator(RegistryAccess registryAccess, long seed)
        {
            return new HexChunkGenerator(
                registryAccess.registryOrThrow(Registry.STRUCTURE_SET_REGISTRY),
                registryAccess.registryOrThrow(Registry.NOISE_REGISTRY),
                registryAccess.registryOrThrow(Registry.DENSITY_FUNCTION_REGISTRY),
                MultiNoiseBiomeSource.Preset.OVERWORLD.biomeSource(
                    registryAccess.registryOrThrow(Registry.BIOME_REGISTRY)
                ),
                seed,
                registryAccess.registryOrThrow(Registry.NOISE_GENERATOR_SETTINGS_REGISTRY).getHolderOrThrow(NoiseGeneratorSettings.OVERWORLD),
                HexSettings.OVERWORLD
            );
        }
/*
        @Override
        public WorldGenSettings createSettings(RegistryAccess registryAccess, long seed, boolean generateStructures, boolean bonusChest, String generatorSettings)
        {
            if (overworldOnly)
            {
                return ForgeWorldPreset.IBasicChunkGeneratorFactory.super.createSettings(registryAccess, seed, generateStructures, bonusChest, generatorSettings);
            }

            final Registry<Biome> biomeRegistry = registryAccess.registryOrThrow(Registry.BIOME_REGISTRY);
            final Registry<DimensionType> dimensionTypeRegistry = registryAccess.registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY);
            final Registry<NoiseGeneratorSettings> noiseSettingsRegistry = registryAccess.registryOrThrow(Registry.NOISE_GENERATOR_SETTINGS_REGISTRY);

            final ChunkGenerator overworld = createChunkGenerator(biomeRegistry, noiseSettingsRegistry, seed, generatorSettings);
            final ChunkGenerator nether = createNetherChunkGenerator(biomeRegistry, noiseSettingsRegistry, seed);
            final ChunkGenerator end = createEndChunkGenerator(biomeRegistry, noiseSettingsRegistry, seed);


            final MappedRegistry<LevelStem> levels = new MappedRegistry<>(Registry.LEVEL_STEM_REGISTRY, Lifecycle.experimental());
            levels.register(LevelStem.NETHER, new LevelStem(() -> dimensionTypeRegistry.getOrThrow(DimensionType.NETHER_LOCATION), nether), Lifecycle.stable());
            levels.register(LevelStem.END, new LevelStem(() -> dimensionTypeRegistry.getOrThrow(DimensionType.END_LOCATION), end), Lifecycle.stable());

            return new WorldGenSettings(seed, generateStructures, bonusChest, WorldGenSettings.withOverworld(dimensionTypeRegistry, levels, overworld));
        }

        private ChunkGenerator createOverworldChunkGenerator(Registry<Biome> biomeRegistry, Registry<NoiseGeneratorSettings> noiseSettingsRegistry, long seed)
        {
            final BiomeSource overworld = getModdedBiomeSource(HexLandsConfig.COMMON.useBoPOverworld.get(), "biomesoplenty", "biomesoplenty.common.world.BOPBiomeProvider", new Class<?>[] {long.class, Registry.class}, seed, biomeRegistry)
                    .orElseGet(() -> new OverworldBiomeSource(seed, false, false, biomeRegistry));
            final HexBiomeSource hex = new HexBiomeSource(overworld, biomeRegistry, HexSettings.OVERWORLD);
            return new HexChunkGenerator(hex, seed, () -> noiseSettingsRegistry.getOrThrow(NoiseGeneratorSettings.OVERWORLD));
        }

        private ChunkGenerator createNetherChunkGenerator(Registry<Biome> biomeRegistry, Registry<NoiseGeneratorSettings> noiseSettingsRegistry, long seed)
        {
            final BiomeSource nether = getModdedBiomeSource(HexLandsConfig.COMMON.useBoPNether.get(), "biomesoplenty", "biomesoplenty.common.world.BOPNetherBiomeProvider", new Class<?>[] {long.class, Registry.class}, seed, biomeRegistry)
                .orElseGet(() -> DimensionType.defaultNetherGenerator(biomeRegistry, noiseSettingsRegistry, seed).getBiomeSource());
            final HexBiomeSource hex = new HexBiomeSource(nether, biomeRegistry, HexSettings.NETHER);
            return new HexChunkGenerator(hex, seed, () -> noiseSettingsRegistry.getOrThrow(NoiseGeneratorSettings.NETHER));
        }

        private ChunkGenerator createEndChunkGenerator(Registry<Biome> biomeRegistry, Registry<NoiseGeneratorSettings> noiseSettingsRegistry, long seed)
        {
            final BiomeSource end = DimensionType.defaultEndGenerator(biomeRegistry, noiseSettingsRegistry, seed).getBiomeSource();
            final HexBiomeSource hex = HexLandsConfig.COMMON.preserveMainEndIsland.get() ? new HexEndBiomeSource(end, biomeRegistry, HexSettings.END) : new HexBiomeSource(end, biomeRegistry, HexSettings.END);
            return new HexChunkGenerator(hex, seed, () -> noiseSettingsRegistry.getOrThrow(NoiseGeneratorSettings.END));
        }

        private Optional<BiomeSource> getModdedBiomeSource(boolean configOption, String predicateModId, String className, Class<?>[] constructorTypes, Object... constructorParams)
        {
            if (!configOption)
            {
                return Optional.empty();
            }
            if (!ModList.get().isLoaded(predicateModId))
            {
                LOGGER.warn("Mod {} is not loaded, skipping integration.", predicateModId);
                return Optional.empty();
            }
            try
            {
                final Class<?> cls = Class.forName(className);
                final Constructor<?> ctor = cls.getConstructor(constructorTypes);
                final BiomeSource biomeSource = (BiomeSource) ctor.newInstance(constructorParams);
                return Optional.of(biomeSource);
            }
            catch (Exception e)
            {
                LOGGER.warn("Unable to find biome source from mod {}: {}. Please inform HexLands about this!", predicateModId, e.getMessage());
                LOGGER.debug("Exception", e);
                return Optional.empty();
            }
        }*/
    }
}
