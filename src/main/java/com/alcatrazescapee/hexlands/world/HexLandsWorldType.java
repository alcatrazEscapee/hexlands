/*
 * Part of the HexLands mod.
 * Licensed under MIT. See the project LICENSE.txt for details.
 */

package com.alcatrazescapee.hexlands.world;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.SimpleRegistry;
import net.minecraft.world.Dimension;
import net.minecraft.world.DimensionType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.provider.BiomeProvider;
import net.minecraft.world.biome.provider.EndBiomeProvider;
import net.minecraft.world.biome.provider.NetherBiomeProvider;
import net.minecraft.world.biome.provider.OverworldBiomeProvider;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.DimensionSettings;
import net.minecraft.world.gen.settings.DimensionGeneratorSettings;
import net.minecraftforge.common.ForgeConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.world.ForgeWorldType;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import com.alcatrazescapee.hexlands.HexLands;
import com.alcatrazescapee.hexlands.util.HexLandsConfig;
import com.mojang.serialization.Lifecycle;

@SuppressWarnings("unused")
public class HexLandsWorldType
{
    public static final DeferredRegister<ForgeWorldType> WORLD_TYPES = DeferredRegister.create(ForgeRegistries.WORLD_TYPES, HexLands.MOD_ID);

    public static final RegistryObject<ForgeWorldType> HEX_LANDS = register("hexlands", () -> new Impl(false));
    public static final RegistryObject<ForgeWorldType> HEX_LANDS_OVERWORLD_ONLY = register("hexlands_overworld_only", () -> new Impl(true));

    private static final Logger LOGGER = LogManager.getLogger();

    private static final Method DEFAULT_NETHER_GENERATOR = ObfuscationReflectionHelper.findMethod(DimensionType.class, "func_242720_b", Registry.class, Registry.class, long.class); // defaultNetherGenerator
    private static final Method DEFAULT_END_GENERATOR = ObfuscationReflectionHelper.findMethod(DimensionType.class, "func_242717_a", Registry.class, Registry.class, long.class); // defaultEndGenerator

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void setDefault()
    {
        if (HexLandsConfig.COMMON.setHexLandsWorldTypeAsDefault.get() && ForgeConfig.COMMON.defaultWorldType.get().equals("default"))
        {
            ((ForgeConfigSpec.ConfigValue) ForgeConfig.COMMON.defaultWorldType).set(HexLandsWorldType.HEX_LANDS.getId().toString());
        }
    }

    private static RegistryObject<ForgeWorldType> register(String name, Supplier<ForgeWorldType.IChunkGeneratorFactory> factory)
    {
        return WORLD_TYPES.register(name, () -> new ForgeWorldType(factory.get()));
    }

    static class Impl implements ForgeWorldType.IBasicChunkGeneratorFactory
    {
        private final boolean overworldOnly;

        Impl(boolean overworldOnly)
        {
            this.overworldOnly = overworldOnly;
        }

        @Override
        public ChunkGenerator createChunkGenerator(Registry<Biome> biomeRegistry, Registry<DimensionSettings> dimensionSettingsRegistry, long seed)
        {
            return createOverworldChunkGenerator(biomeRegistry, dimensionSettingsRegistry, seed);
        }

        @Override
        public DimensionGeneratorSettings createSettings(DynamicRegistries registryAccess, long seed, boolean generateStructures, boolean bonusChest, String generatorSettings)
        {
            if (overworldOnly)
            {
                return ForgeWorldType.IBasicChunkGeneratorFactory.super.createSettings(registryAccess, seed, generateStructures, bonusChest, generatorSettings);
            }

            final Registry<Biome> biomeRegistry = registryAccess.registryOrThrow(Registry.BIOME_REGISTRY);
            final Registry<DimensionType> dimensionTypeRegistry = registryAccess.registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY);
            final Registry<DimensionSettings> dimensionSettingsRegistry = registryAccess.registryOrThrow(Registry.NOISE_GENERATOR_SETTINGS_REGISTRY);

            final ChunkGenerator overworld = createChunkGenerator(biomeRegistry, dimensionSettingsRegistry, seed, generatorSettings);
            final ChunkGenerator nether = createNetherChunkGenerator(biomeRegistry, dimensionSettingsRegistry, seed);
            final ChunkGenerator end = createEndChunkGenerator(biomeRegistry, dimensionSettingsRegistry, seed);

            final SimpleRegistry<Dimension> dimensions = new SimpleRegistry<>(Registry.LEVEL_STEM_REGISTRY, Lifecycle.experimental());
            dimensions.register(Dimension.NETHER, new Dimension(() -> dimensionTypeRegistry.getOrThrow(DimensionType.NETHER_LOCATION), nether), Lifecycle.stable());
            dimensions.register(Dimension.END, new Dimension(() -> dimensionTypeRegistry.getOrThrow(DimensionType.END_LOCATION), end), Lifecycle.stable());

            return new DimensionGeneratorSettings(seed, generateStructures, bonusChest, DimensionGeneratorSettings.withOverworld(dimensionTypeRegistry, dimensions, overworld));
        }

        private ChunkGenerator createOverworldChunkGenerator(Registry<Biome> biomeRegistry, Registry<DimensionSettings> dimensionSettingsRegistry, long seed)
        {
            final BiomeProvider overworld =
                getModdedBiomeSource(HexLandsConfig.COMMON.useBoPOverworld.get(), "biomesoplenty", "biomesoplenty.common.world.BOPBiomeProvider", new Class<?>[] {long.class, Registry.class}, seed, biomeRegistry)
                    .orElseGet(() -> new OverworldBiomeProvider(seed, false, false, biomeRegistry));
            final HexBiomeSource hex = new HexBiomeSource(overworld, biomeRegistry, HexSettings.OVERWORLD, seed);
            return new HexChunkGenerator(hex, () -> dimensionSettingsRegistry.getOrThrow(DimensionSettings.OVERWORLD), seed);
        }

        private ChunkGenerator createNetherChunkGenerator(Registry<Biome> biomeRegistry, Registry<DimensionSettings> dimensionSettingsRegistry, long seed)
        {
            final BiomeProvider nether =
                getModdedBiomeSource(HexLandsConfig.COMMON.useBoPNether.get(), "biomesoplenty", "biomesoplenty.common.world.BOPNetherBiomeProvider", new Class<?>[] {long.class, Registry.class}, seed, biomeRegistry)
                    .orElseGet(() -> getDefaultChunkGenerator(DEFAULT_NETHER_GENERATOR, biomeRegistry, dimensionSettingsRegistry, seed).map(ChunkGenerator::getBiomeSource)
                        .orElseGet(() -> NetherBiomeProvider.Preset.NETHER.biomeSource(biomeRegistry, seed)));
            final HexBiomeSource hex = new HexBiomeSource(nether, biomeRegistry, HexSettings.NETHER, seed);
            return new HexChunkGenerator(hex, () -> dimensionSettingsRegistry.getOrThrow(DimensionSettings.NETHER), seed);
        }

        private ChunkGenerator createEndChunkGenerator(Registry<Biome> biomeRegistry, Registry<DimensionSettings> dimensionSettingsRegistry, long seed)
        {
            final BiomeProvider end =
                getDefaultChunkGenerator(DEFAULT_END_GENERATOR, biomeRegistry, dimensionSettingsRegistry, seed).map(ChunkGenerator::getBiomeSource)
                    .orElseGet(() -> new EndBiomeProvider(biomeRegistry, seed));
            final HexBiomeSource hex = new HexBiomeSource(end, biomeRegistry, HexSettings.END, seed);
            return new HexChunkGenerator(hex, () -> dimensionSettingsRegistry.getOrThrow(DimensionSettings.END), seed);
        }

        private Optional<ChunkGenerator> getDefaultChunkGenerator(Method defaultMethod, Object... params)
        {
            try
            {
                return Optional.of((ChunkGenerator) defaultMethod.invoke(null, params));
            }
            catch (Exception e)
            {
                LOGGER.warn("Failed to get default chunk generator: {}", e.getMessage());
                LOGGER.debug("Exception", e);
                return Optional.empty();
            }
        }

        private Optional<BiomeProvider> getModdedBiomeSource(boolean configOption, String predicateModId, String className, Class<?>[] constructorTypes, Object... constructorParams)
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
                final BiomeProvider biomeSource = (BiomeProvider) ctor.newInstance(constructorParams);
                return Optional.of(biomeSource);
            }
            catch (Exception e)
            {
                LOGGER.warn("Unable to find biome source from mod {}: {}", predicateModId, e.getMessage());
                LOGGER.debug("Exception", e);
                return Optional.empty();
            }
        }
    }
}
