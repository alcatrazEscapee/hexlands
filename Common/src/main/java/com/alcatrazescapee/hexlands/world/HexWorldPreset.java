package com.alcatrazescapee.hexlands.world;

import com.mojang.serialization.Lifecycle;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.WritableRegistry;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

import com.alcatrazescapee.hexlands.util.HexSettings;

public record HexWorldPreset(boolean overworldOnly)
{
    public static final HexWorldPreset HEXLANDS = new HexWorldPreset(false);
    public static final HexWorldPreset HEXLANDS_OVERWORLD_ONLY = new HexWorldPreset(true);

    public ChunkGenerator createChunkGenerator(RegistryAccess registryAccess, long seed)
    {
        return new HexChunkGenerator(
            registryAccess.registryOrThrow(Registry.STRUCTURE_SET_REGISTRY),
            registryAccess.registryOrThrow(Registry.NOISE_REGISTRY),
            MultiNoiseBiomeSource.Preset.OVERWORLD.biomeSource(
                registryAccess.registryOrThrow(Registry.BIOME_REGISTRY)
            ),
            seed,
            registryAccess.registryOrThrow(Registry.NOISE_GENERATOR_SETTINGS_REGISTRY).getHolderOrThrow(NoiseGeneratorSettings.OVERWORLD),
            HexSettings.OVERWORLD
        );
    }

    public WorldGenSettings createSettings(RegistryAccess registryAccess, long seed, boolean generateStructures, boolean bonusChest)
    {
        final Registry<DimensionType> dimensionTypeRegistry = registryAccess.registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY);

        if (overworldOnly)
        {
            return new WorldGenSettings(seed, generateStructures, bonusChest, WorldGenSettings.withOverworld(dimensionTypeRegistry, DimensionType.defaultDimensions(registryAccess, seed), createChunkGenerator(registryAccess, seed)));
        }

        final Registry<NoiseGeneratorSettings> noiseGeneratorSettings = registryAccess.registryOrThrow(Registry.NOISE_GENERATOR_SETTINGS_REGISTRY);
        final Registry<StructureSet> structureSets = registryAccess.registryOrThrow(Registry.STRUCTURE_SET_REGISTRY);
        final Registry<NormalNoise.NoiseParameters> noiseParameters = registryAccess.registryOrThrow(Registry.NOISE_REGISTRY);

        final Registry<LevelStem> defaultDimensions = DimensionType.defaultDimensions(registryAccess, seed);
        final LevelStem defaultNether = defaultDimensions.getOrThrow(LevelStem.NETHER);
        final LevelStem defaultEnd = defaultDimensions.getOrThrow(LevelStem.END);

        final WritableRegistry<LevelStem> dimensions = new MappedRegistry<>(Registry.LEVEL_STEM_REGISTRY, Lifecycle.experimental(), null);

        dimensions.register(LevelStem.NETHER, new LevelStem(defaultNether.typeHolder(), new HexChunkGenerator(structureSets, noiseParameters, defaultNether.generator().getBiomeSource(), seed, noiseGeneratorSettings.getOrCreateHolder(NoiseGeneratorSettings.NETHER), HexSettings.NETHER)), Lifecycle.stable());
        dimensions.register(LevelStem.END, new LevelStem(defaultEnd.typeHolder(), new HexChunkGenerator(structureSets, noiseParameters, defaultEnd.generator().getBiomeSource(), seed, noiseGeneratorSettings.getOrCreateHolder(NoiseGeneratorSettings.END), HexSettings.END)), Lifecycle.stable());

        return new WorldGenSettings(seed, generateStructures, bonusChest, WorldGenSettings.withOverworld(dimensionTypeRegistry, dimensions, createChunkGenerator(registryAccess, seed)));
    }
}
