/*
 * Part of the HexLands mod.
 * Licensed under MIT. See the project LICENSE.txt for details.
 */

package com.alcatrazescapee.hexlands.world;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.DoubleFunction;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.surfacebuilders.SurfaceBuilder;
import net.minecraft.world.level.levelgen.surfacebuilders.SurfaceBuilderBaseConfiguration;

import com.alcatrazescapee.hexlands.util.Hex;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.HashCommon;

@SuppressWarnings("deprecation")
public class HexChunkGenerator extends NoiseBasedChunkGenerator
{
    public static final Codec<HexChunkGenerator> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        BiomeSource.CODEC.comapFlatMap(
            source -> {
                if (source instanceof HexBiomeSource hex)
                {
                    return DataResult.success(hex);
                }
                return DataResult.error("HexLands chunk generator requires a HexLands biome source");
            },
            source -> source
        ).fieldOf("biome_source").forGetter(c -> c.hexBiomeSource),
        Codec.LONG.fieldOf("seed").forGetter(c -> c.seed),
        NoiseGeneratorSettings.CODEC.fieldOf("settings").forGetter(c -> c.settings)
    ).apply(instance, HexChunkGenerator::new));

    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    private final HexBiomeSource hexBiomeSource;
    private final HexNoiseSampler hexSampler;

    private final HexSettings hexSettings;
    private final SurfaceBuilder<SurfaceBuilderBaseConfiguration> hexBorderSurfaceBuilder;
    private final SurfaceBuilderBaseConfiguration hexBorderSurfaceBuilderConfig;

    @Nullable private NoiseGeneratorSettings cachedSettings;

    public HexChunkGenerator(HexBiomeSource hexBiomeSource, long seed, Supplier<NoiseGeneratorSettings> settings)
    {
        super(hexBiomeSource, seed, settings);

        final NoiseGeneratorSettings noiseGeneratorSettings = settings.get();
        final NoiseSettings noiseSettings = noiseGeneratorSettings.noiseSettings();

        this.hexBiomeSource = hexBiomeSource;
        this.hexSampler = new HexNoiseSampler(hexBiomeSource, cellWidth, cellHeight, cellCountY, noiseSettings, sampler.blendedNoise, sampler.islandNoise, sampler.depthNoise, sampler.caveNoiseModifier);

        this.hexSettings = hexBiomeSource.hexSettings();
        this.hexBorderSurfaceBuilder = hexSettings.borderExtendsToBedrock() ? new HexBorderSurfaceBuilder(hexSettings.borderState()) : SurfaceBuilder.DEFAULT;
        this.hexBorderSurfaceBuilderConfig = new SurfaceBuilderBaseConfiguration(hexSettings.borderState(), hexSettings.borderState(), hexSettings.borderState());
    }

    @Override
    protected Codec<HexChunkGenerator> codec()
    {
        return CODEC;
    }

    @Override
    public ChunkGenerator withSeed(long seed)
    {
        return new HexChunkGenerator(hexBiomeSource.withSeed(seed), seed, settings);
    }

    @Override
    public void buildSurfaceAndBedrock(WorldGenRegion level, ChunkAccess chunk)
    {
        final WorldgenRandom random = new WorldgenRandom(seed);
        random.setBaseChunkSeed(chunk.getPos().x, chunk.getPos().z);

        final ChunkPos chunkPos = chunk.getPos();
        final int blockX = chunkPos.getMinBlockX(), blockZ = chunkPos.getMinBlockZ();

        final Map<Hex, Biome> biomesByHex = new HashMap<>();

        final double hexScale = hexSettings.biomeScale();
        final double hexSize = hexSettings.hexSize() * hexScale;
        final double hexBorder = hexSettings.hexBorderThreshold();

        for (int localX = 0; localX < 16; ++localX)
        {
            for (int localZ = 0; localZ < 16; ++localZ)
            {
                final int x = blockX + localX;
                final int z = blockZ + localZ;

                final Hex hex = Hex.blockToHex(x * hexScale, z * hexScale, hexSize);
                final Hex adjacentHex = hex.adjacent(x * hexScale, z * hexScale);

                final int y = chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, localX, localZ) + 1;
                final double noise = surfaceNoise.getSurfaceNoiseValue(x * 0.0625D, z * 0.0625D, 0.0625D, localX * 0.0625D) * 15.0D;
                final Biome biome = biomesByHex.computeIfAbsent(hex, hexBiomeSource::getHexBiome);
                if (hex.radius(x * hexScale, z * hexScale) >= hexBorder)
                {
                    final Biome adjacentBiome = biomesByHex.computeIfAbsent(adjacentHex, hexBiomeSource::getHexBiome);
                    if (adjacentBiome != biome)
                    {
                        hexBorderSurfaceBuilder.apply(random, chunk, biome, x, z, y, noise, defaultBlock, defaultFluid, getSeaLevel(), settings().getMinSurfaceLevel(), level.getSeed(), hexBorderSurfaceBuilderConfig);
                        continue;
                    }
                }
                // Otherwise
                biome.buildSurfaceAt(random, chunk, x, z, y, noise, defaultBlock, defaultFluid, getSeaLevel(), settings().getMinSurfaceLevel(), level.getSeed());
            }
        }
        setBedrock(chunk, random);
    }

    @Override
    protected ChunkAccess doFill(StructureFeatureManager structureManager, ChunkAccess chunk, int minCellY, int cellCountY)
    {
        final Heightmap oceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        final Heightmap worldSurface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        final ChunkPos chunkPos = chunk.getPos();
        final int chunkX = chunkPos.getMinBlockX();
        final int chunkZ = chunkPos.getMinBlockZ();
        final Beardifier beardifier = new Beardifier(structureManager, chunk) {};
        final Aquifer aquifer = getAquifer(minCellY, cellCountY, chunkPos);
        final NoiseInterpolator mainNoiseInterpolator = new NoiseInterpolator(cellCountX, cellCountY, cellCountZ, chunkPos, minCellY, this::fillNoiseColumn);
        final List<NoiseInterpolator> interpolators = new ArrayList<>();
        interpolators.add(mainNoiseInterpolator);
        final DoubleFunction<BaseStoneSource> baseStoneSource = createBaseStoneSource(minCellY, chunkPos, interpolators::add);
        final DoubleFunction<NoiseModifier> caveNoiseModifier = createCaveNoiseModifier(minCellY, chunkPos, interpolators::add);

        final Hex[] hexes = new Hex[16 * 16], adjacentHexes = new Hex[16 * 16];
        final Map<Hex, Biome> biomesByHex = new HashMap<>();
        final Random hexBorderRandom = new Random();

        final double hexScale = hexSettings.biomeScale();
        final double hexSize = hexSettings.hexSize() * hexScale;
        final double hexBorder = hexSettings.hexBorderThreshold();

        // Iterate each chunk position and identify the containing hex, and adjacent hex
        for (int x = 0; x < 16; x++)
        {
            for (int z = 0; z < 16; z++)
            {
                final int hx = chunkX + x, hz = chunkZ + z;
                final Hex hex = Hex.blockToHex(hx * hexScale, hz * hexScale, hexSize);
                final Hex adjacent = hex.adjacent(hx * hexScale, hz * hexScale);

                hexes[x + 16 * z] = hex;
                adjacentHexes[x + 16 * z] = adjacent;

                // Sample biomes once per each hex we encounter in the main grid.
                biomesByHex.computeIfAbsent(hex, hexBiomeSource::getHexBiome);
                biomesByHex.computeIfAbsent(adjacent, hexBiomeSource::getHexBiome);
            }
        }

        interpolators.forEach(NoiseInterpolator::initializeForFirstCellX);

        final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int cellX = 0; cellX < cellCountX; ++cellX)
        {
            final int finalCellX = cellX;
            interpolators.forEach(interpolator -> interpolator.advanceCellX(finalCellX));

            for (int cellZ = 0; cellZ < cellCountZ; ++cellZ)
            {
                LevelChunkSection section = chunk.getOrCreateSection(chunk.getSectionsCount() - 1);
                for (int cellY = cellCountY - 1; cellY >= 0; --cellY)
                {
                    final int finalCellZ = cellZ, finalCellY = cellY;
                    interpolators.forEach(interpolator -> interpolator.selectCellYZ(finalCellY, finalCellZ));

                    for (int localCellY = cellHeight - 1; localCellY >= 0; --localCellY)
                    {
                        final int y = (minCellY + cellY) * cellHeight + localCellY;
                        final int localY = y & 15;

                        final int sectionIndex = chunk.getSectionIndex(y);
                        if (chunk.getSectionIndex(section.bottomBlockY()) != sectionIndex)
                        {
                            section = chunk.getOrCreateSection(sectionIndex);
                        }

                        final double deltaY = (double) localCellY / cellHeight;
                        interpolators.forEach(interpolator -> interpolator.updateForY(deltaY));

                        for (int localCellX = 0; localCellX < cellWidth; ++localCellX)
                        {
                            final int x = chunkX + cellX * cellWidth + localCellX;
                            final int localX = x & 15;
                            final double deltaX = (double) localCellX / cellWidth;
                            interpolators.forEach(interpolator -> interpolator.updateForX(deltaX));

                            for (int localCellZ = 0; localCellZ < cellWidth; ++localCellZ)
                            {
                                final int z = chunkZ + cellZ * cellWidth + localCellZ;
                                final int localZ = z & 15;
                                final double deltaZ = (double) localCellZ / cellWidth;

                                double noise = mainNoiseInterpolator.calculateValue(deltaZ);

                                // Hex borders
                                final Hex hex = hexes[localX + localZ * 16];
                                if (hex.radius(x * hexScale, z * hexScale) > hexBorder)
                                {
                                    final Biome biome = biomesByHex.get(hex);
                                    final Hex adjacentHex = adjacentHexes[localX + localZ * 16];
                                    final Biome adjacentBiome = biomesByHex.get(adjacentHex);

                                    assert biome != null;
                                    assert adjacentBiome != null;

                                    if (biome != adjacentBiome) // No borders between adjacent hexes of the same biome
                                    {
                                        noise = applyHexBorderNoiseModifier(hex, biome, hexBorderRandom, y);
                                    }
                                }

                                BlockState state = updateNoiseAndGenerateBaseState(beardifier, aquifer, baseStoneSource.apply(deltaZ), caveNoiseModifier.apply(deltaZ), x, y, z, noise);
                                if (state != AIR)
                                {
                                    if (state.getLightEmission() != 0 && chunk instanceof ProtoChunk)
                                    {
                                        cursor.set(x, y, z);
                                        ((ProtoChunk) chunk).addLight(cursor);
                                    }

                                    section.setBlockState(localX, localY, localZ, state, false);
                                    oceanFloor.update(localX, y, localZ, state);
                                    worldSurface.update(localX, y, localZ, state);
                                    if (aquifer.shouldScheduleFluidUpdate() && !state.getFluidState().isEmpty())
                                    {
                                        cursor.set(x, y, z);
                                        chunk.getLiquidTicks().scheduleTick(cursor, state.getFluidState().getType(), 0);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            interpolators.forEach(NoiseInterpolator::swapSlices);
        }
        return chunk;
    }

    @Override
    protected void fillNoiseColumn(double[] column, int cellX, int cellY, int minCellY, int cellCountY)
    {
        hexSampler.fillNoiseColumn(column, cellX, cellY, settings().noiseSettings(), getSeaLevel(), minCellY, cellCountY);
    }

    protected NoiseGeneratorSettings settings()
    {
        if (cachedSettings == null)
        {
            cachedSettings = settings.get();
        }
        return cachedSettings;
    }

    protected double applyHexBorderNoiseModifier(Hex hex, Biome biome, Random random, int y)
    {
        if (hexSettings.noBorder())
        {
            return -1;
        }

        random.setSeed(HashCommon.murmurHash3(hex.hashCode()));
        if (hexSettings.windowedBorder())
        {
            final int roof = height + 1 - settings().getBedrockRoofPosition() - 30 - random.nextInt(10) - random.nextInt(10);
            final int floor = settings().getBedrockFloorPosition() + 30 + random.nextInt(5) + random.nextInt(5);
            return y < floor || y > roof ? 1 : -1;
        }
        else
        {
            final double biomeHeight = getBaseBiomeHeight(biome) * cellHeight;
            final int adjustedHeight = (int) (biomeHeight + random.nextInt(4) - random.nextInt(4));
            return y < adjustedHeight ? 1 : -1;
        }
    }

    protected double getBaseBiomeHeight(Biome biome)
    {
        // No random density offset, since we don't take into account the cellX/Z of the hex center
        final NoiseSettings noiseSettings = settings().noiseSettings();
        final double densityFactor = noiseSettings.densityFactor();
        final double densityOffset = noiseSettings.densityOffset();

        double depth = biome.getDepth();
        if (noiseSettings.isAmplified() && depth > 0.0F)
        {
            depth = 1.0F + depth * 2.0F;
        }
        depth = (depth * 0.5F - 0.125F) * 0.265625D;

        // cellY
        return (1 + (depth + densityOffset) / densityFactor) * (cellCountY / 2.0D);
    }
}
