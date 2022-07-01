/*
 * Part of the HexLands mod.
 * Licensed under MIT. See the project LICENSE.txt for details.
 */

package com.alcatrazescapee.hexlands.world;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.UnaryOperator;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.StructureSet;

import com.alcatrazescapee.hexlands.mixin.NoiseBasedChunkGeneratorAccessor;
import com.alcatrazescapee.hexlands.platform.XPlatform;
import com.alcatrazescapee.hexlands.util.Hex;
import com.alcatrazescapee.hexlands.util.HexSettings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public class HexChunkGenerator extends ChunkGenerator
{
    public static final Codec<HexChunkGenerator> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        RegistryOps.retrieveRegistry(Registry.STRUCTURE_SET_REGISTRY).forGetter(c -> c.structureSets),
        RegistryOps.retrieveRegistry(Registry.NOISE_REGISTRY).forGetter(c -> c.noiseParameters),
        BiomeSource.CODEC.fieldOf("biome_source").forGetter(c -> c.biomeSource),
        Codec.LONG.fieldOf("seed").forGetter(c -> c.seed),
        NoiseGeneratorSettings.CODEC.fieldOf("settings").forGetter(c -> c.settings),
        HexSettings.CODEC.fieldOf("hex_settings").forGetter(c -> c.hexSettings)
    ).apply(instance, HexChunkGenerator::new));

    private final NoiseBasedChunkGenerator noiseChunkGenerator;
    private final Registry<NormalNoise.NoiseParameters> noiseParameters;
    private final Holder<NoiseGeneratorSettings> settings;
    private final HexSettings hexSettings;
    private final long seed;

    private final Climate.Sampler hexClimateSampler;

    public HexChunkGenerator(Registry<StructureSet> structureSets, Registry<NormalNoise.NoiseParameters> noiseParameters, BiomeSource biomeSource, long seed, Holder<NoiseGeneratorSettings> settings, HexSettings hexSettings)
    {
        super(structureSets, Optional.empty(), biomeSource);

        this.noiseChunkGenerator = new NoiseBasedChunkGenerator(structureSets, noiseParameters, biomeSource, seed, settings);

        if (biomeSource instanceof HexEndBiomeSource endBiomeSource)
        {
            endBiomeSource.applySettings(hexSettings);
        }

        this.noiseParameters = noiseParameters;
        this.settings = settings;
        this.seed = seed;

        final DensityFunction.Visitor visitor = f -> {
            if (XPlatform.INSTANCE.isNoiseDensityFunction(f))
            {
                return sampleHexRelative(f);
            }
            return f;
        };

        final NoiseRouterWithOnlyNoises noises = settings.value().noiseRouter();
        final NoiseRouterWithOnlyNoises hexNoises = new NoiseRouterWithOnlyNoises(
            noises.barrierNoise(),
            noises.fluidLevelFloodednessNoise(),
            noises.fluidLevelSpreadNoise(),
            noises.lavaNoise(),
            sampleHexCenter(noises.temperature()),
            sampleHexCenter(noises.vegetation()),
            sampleHexCenter(noises.continents()),
            sampleHexCenter(noises.erosion()),
            sampleHexCenter(noises.depth()),
            sampleHexCenter(noises.ridges()),
            noises.initialDensityWithoutJaggedness().mapAll(visitor),
            noises.finalDensity().mapAll(visitor),
            noises.veinToggle(),
            noises.veinRidged(),
            noises.veinGap()
        );

        final NoiseRouter hexRouter = NoiseRouterData.createNoiseRouter(settings.value().noiseSettings(), seed, noiseParameters, settings.value().getRandomSource(), hexNoises);

        this.hexSettings = hexSettings;
        this.hexClimateSampler = new Climate.Sampler(
            hexRouter.temperature(),
            hexRouter.humidity(),
            hexRouter.continents(),
            hexRouter.erosion(),
            hexRouter.depth(),
            hexRouter.ridges(),
            hexRouter.spawnTarget()
        );

        final NoiseBasedChunkGeneratorAccessor mutableAccess = (NoiseBasedChunkGeneratorAccessor) (Object) this.noiseChunkGenerator;

        mutableAccess.setRouter(hexRouter);
        mutableAccess.setSampler(hexClimateSampler);
    }

    @Override
    protected Codec<HexChunkGenerator> codec()
    {
        return CODEC;
    }

    @Override
    public ChunkGenerator withSeed(long seed)
    {
        return new HexChunkGenerator(structureSets, noiseParameters, biomeSource.withSeed(seed), seed, settings, hexSettings);
    }

    @Override
    public CompletableFuture<ChunkAccess> createBiomes(Registry<Biome> biomeRegistry, Executor executor, Blender blender, StructureFeatureManager structureFeatureManager, ChunkAccess chunk)
    {
        return noiseChunkGenerator.createBiomes(biomeRegistry, executor, blender, structureFeatureManager, chunk);
    }

    @Override
    public Climate.Sampler climateSampler()
    {
        return hexClimateSampler;
    }

    @Override
    public void applyCarvers(WorldGenRegion level, long seed, BiomeManager biomeManager, StructureFeatureManager structureFeatureManager, ChunkAccess chunk, GenerationStep.Carving step)
    {
        noiseChunkGenerator.applyCarvers(level, seed, biomeManager, structureFeatureManager, chunk, step);
    }

    @Override
    public void buildSurface(WorldGenRegion level, StructureFeatureManager structureFeatureManager, ChunkAccess chunk)
    {
        noiseChunkGenerator.buildSurface(level, structureFeatureManager, chunk);

        applyAtHexBorders(chunk, (cursor, placed) -> {

            // Bottom Border
            for (int y = placed.minY; y <= placed.borderMinY; y++)
            {
                final Block block = chunk.getBlockState(cursor.setY(y)).getBlock();
                if (block != Blocks.BEDROCK)
                {
                    chunk.setBlockState(cursor, placed.borderMinState, false);
                }
            }

            // Between Borders
            for (int y = placed.borderMinY + 1; y < placed.borderMaxY; y++)
            {
                chunk.setBlockState(cursor.setY(y), Blocks.AIR.defaultBlockState(), false);
            }

            // Top Border
            for (int y = placed.borderMaxY; y <= placed.maxY; y++)
            {
                final Block block = chunk.getBlockState(cursor.setY(y)).getBlock();
                if (block != Blocks.BEDROCK)
                {
                    chunk.setBlockState(cursor, placed.borderMinState, false);
                }
            }
        });
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion level)
    {
        noiseChunkGenerator.spawnOriginalMobs(level);
    }

    @Override
    public int getGenDepth()
    {
        return noiseChunkGenerator.getGenDepth();
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender, StructureFeatureManager structureFeatureManager, ChunkAccess chunk)
    {
        return noiseChunkGenerator.fillFromNoise(executor, blender, structureFeatureManager, chunk);
    }

    @Override
    public int getSeaLevel()
    {
        return noiseChunkGenerator.getSeaLevel();
    }

    @Override
    public int getMinY()
    {
        return noiseChunkGenerator.getMinY();
    }

    @Override
    public int getBaseHeight(int blockX, int blockZ, Heightmap.Types type, LevelHeightAccessor level)
    {
        return noiseChunkGenerator.getBaseHeight(blockX, blockX, type, level);
    }

    @Override
    public NoiseColumn getBaseColumn(int blockX, int blockZ, LevelHeightAccessor level)
    {
        return noiseChunkGenerator.getBaseColumn(blockX, blockX, level);
    }

    @Override
    public void addDebugScreenInfo(List<String> tooltips, BlockPos pos)
    {
        final double hexScale = hexSettings.biomeScale();
        final double hexSize = hexSettings.hexSize() * hexScale;
        final Hex hex = Hex.blockToHex(pos.getX() * hexScale, pos.getZ() * hexScale, hexSize);
        final PlacedHex placed = placeHex(hex);

        tooltips.add(String.format("Hex (%d, %d) at %s : H%d B%d-%d", hex.q(), hex.r(), placed.biome().unwrap().map(ResourceKey::location, e -> "[unregistered biome]"), (int) placed.preliminaryHeight, placed.borderMinY, placed.borderMaxY));
        noiseChunkGenerator.addDebugScreenInfo(tooltips, pos);
    }

    private void applyAtHexBorders(ChunkAccess chunk, ColumnApplier applier)
    {
        final Map<Hex, PlacedHex> cachedBiomesByHex = new HashMap<>();
        final ChunkPos chunkPos = chunk.getPos();
        final int blockX = chunkPos.getMinBlockX(), blockZ = chunkPos.getMinBlockZ();

        final double hexScale = hexSettings.biomeScale();
        final double hexSize = hexSettings.hexSize() * hexScale;
        final double hexBorder = hexSettings.hexBorderThreshold();

        final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int localX = 0; localX < 16; ++localX)
        {
            for (int localZ = 0; localZ < 16; ++localZ)
            {
                final int x = blockX + localX;
                final int z = blockZ + localZ;

                final Hex hex = Hex.blockToHex(x * hexScale, z * hexScale, hexSize);
                final Hex adjacentHex = hex.adjacent(x * hexScale, z * hexScale);

                final int y = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, localX, localZ) + 1;
                final PlacedHex placed = cachedBiomesByHex.computeIfAbsent(hex, this::placeHex);
                if (hex.radius(x * hexScale, z * hexScale) >= hexBorder)
                {
                    final PlacedHex adjacentPlacedHex = cachedBiomesByHex.computeIfAbsent(adjacentHex, this::placeHex);
                    if (placed.biome != adjacentPlacedHex.biome)
                    {
                        cursor.setX(x).setZ(z);
                        applier.apply(cursor, placed);
                    }
                }
            }
        }
    }

    private PlacedHex placeHex(Hex hex)
    {
        final BlockPos center = hex.center();
        final double hexScale = hexSettings.biomeScale();
        final int quartX = QuartPos.fromBlock((int) (center.getX() / hexScale));
        final int quartZ = QuartPos.fromBlock((int) (center.getZ() / hexScale));
        final NoiseSettings noiseSettings = settings.value().noiseSettings();
        final double preliminaryHeight = NoiseRouterAccessor.computePreliminarySurfaceLevelScanning(noiseSettings, noiseChunkGenerator.router().initialDensityWithoutJaggedness(), quartX, quartZ);
        final Holder<Biome> biome = biomeSource.getNoiseBiome(quartX, QuartPos.fromBlock((int) preliminaryHeight), quartZ, climateSampler());
        final RandomSource random = new XoroshiroRandomSource(hex.q() * 178293412341L, hex.r() * 7520351231L);

        final int minY = noiseSettings.minY();
        final int maxY = noiseSettings.minY() + noiseSettings.height() - 1;

        final int borderMinY = hexSettings.bottomBorder()
            .map(border -> border.sample(random))
            .orElse(minY - 1);

        final int borderMaxY = hexSettings.topBorder()
            .map(border -> border.sample(random))
            .orElse(maxY + 1);

        final BlockState minBorderState = hexSettings.bottomBorder().map(HexSettings.BorderSettings::state).orElse(Blocks.AIR.defaultBlockState());
        final BlockState maxBorderState = hexSettings.bottomBorder().map(HexSettings.BorderSettings::state).orElse(Blocks.AIR.defaultBlockState());

        return new PlacedHex(hex, biome, preliminaryHeight, minY, maxY, borderMinY, borderMaxY, minBorderState, maxBorderState);
    }

    private DensityFunction sampleHexCenter(DensityFunction function)
    {
        return new PointMapped(function, function.minValue(), function.maxValue(), point -> {
            final double scale = hexSettings.biomeScale();
            final double size = hexSettings.hexSize() * scale;
            final Hex hex = Hex.blockToHex(point.blockX() * scale, point.blockZ() * scale, size);
            final BlockPos center = hex.center();

            return new DensityFunction.SinglePointContext(center.getX(), point.blockY(), center.getZ());
        });
    }

    private DensityFunction sampleHexRelative(DensityFunction function)
    {
        return new PointMapped(function, function.minValue(), function.maxValue(), point -> {
            final double scale = hexSettings.biomeScale();
            final double size = hexSettings.hexSize();
            final Hex hex = Hex.blockToHex(point.blockX() * scale, point.blockZ() * scale, size * scale);
            final BlockPos center = hex.center();

            final double deltaX = point.blockX() - center.getX() / scale;
            final double deltaZ = point.blockZ() - center.getZ() / scale;

            return new DensityFunction.SinglePointContext(center.getX() + (int) deltaX, point.blockY(), center.getZ() + (int) deltaZ);
        });
    }

    record PointMapped(DensityFunction wrapped, double minValue, double maxValue, UnaryOperator<FunctionContext> point) implements DensityFunction.SimpleFunction
    {
        @Override
        public double compute(FunctionContext context)
        {
            return wrapped.compute(point.apply(context));
        }

        @Override
        public DensityFunction mapAll(Visitor visitor)
        {
            return new PointMapped(wrapped.mapAll(visitor), minValue, maxValue, point);
        }

        @Override
        public Codec<? extends DensityFunction> codec()
        {
            return Codec.unit(this);
        }
    }

    record PlacedHex(Hex hex, Holder<Biome> biome, double preliminaryHeight, int minY, int maxY, int borderMinY, int borderMaxY, BlockState borderMinState, BlockState borderMaxState) {}

    @FunctionalInterface
    interface ColumnApplier
    {
        void apply(BlockPos.MutableBlockPos cursor, PlacedHex placed);
    }
}
