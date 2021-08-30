/*
 * Part of the HexLands mod.
 * Licensed under MIT. See the project LICENSE.txt for details.
 */

package com.alcatrazescapee.hexlands.world;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import javax.annotation.Nullable;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityClassification;
import net.minecraft.util.SharedSeedRandom;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.world.Blockreader;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.MobSpawnInfo;
import net.minecraft.world.biome.provider.BiomeProvider;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.*;
import net.minecraft.world.gen.feature.jigsaw.JigsawJunction;
import net.minecraft.world.gen.feature.structure.AbstractVillagePiece;
import net.minecraft.world.gen.feature.structure.StructureManager;
import net.minecraft.world.gen.feature.structure.StructurePiece;
import net.minecraft.world.gen.settings.NoiseSettings;
import net.minecraft.world.gen.surfacebuilders.SurfaceBuilder;
import net.minecraft.world.gen.surfacebuilders.SurfaceBuilderConfig;
import net.minecraft.world.spawner.WorldEntitySpawner;
import net.minecraftforge.common.world.StructureSpawnManager;

import com.alcatrazescapee.hexlands.util.Hex;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;

@SuppressWarnings("deprecation")
public class HexChunkGenerator extends ChunkGenerator
{
    public static final Codec<HexChunkGenerator> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        BiomeProvider.CODEC.comapFlatMap(
            source -> {
                if (source instanceof HexBiomeSource)
                {
                    return DataResult.success((HexBiomeSource) source);
                }
                return DataResult.error("HexLands chunk generator requires a HexLands biome source");
            },
            source -> source
        ).fieldOf("biome_source").forGetter(c -> c.hexBiomeSource),
        DimensionSettings.CODEC.fieldOf("settings").forGetter(c -> c.settings),
        Codec.LONG.fieldOf("seed").forGetter(c -> c.seed)
    ).apply(instance, HexChunkGenerator::new));

    public static final BlockState AIR = Blocks.AIR.defaultBlockState();

    private final HexBiomeSource hexBiomeSource;
    private final Supplier<DimensionSettings> settings;
    private final long seed;

    private final HexSettings hexSettings;
    private final SurfaceBuilder<SurfaceBuilderConfig> hexBorderSurfaceBuilder;
    private final SurfaceBuilderConfig hexBorderSurfaceBuilderConfig;

    private final int height;
    private final int chunkHeight, chunkWidth;
    private final int chunkCountX, chunkCountY, chunkCountZ;

    private final BlockState defaultBlock, defaultFluid;

    private final OctavesNoiseGenerator minLimitPerlinNoise;
    private final OctavesNoiseGenerator maxLimitPerlinNoise;
    private final OctavesNoiseGenerator mainPerlinNoise;
    private final INoiseGenerator surfaceNoise;
    private final OctavesNoiseGenerator depthNoise;

    private DimensionSettings cachedSettings;

    public HexChunkGenerator(HexBiomeSource hexBiomeSource, Supplier<DimensionSettings> settings, long seed)
    {
        super(hexBiomeSource, settings.get().structureSettings());

        this.hexBiomeSource = hexBiomeSource;
        this.settings = settings;
        this.seed = seed;

        this.hexSettings = hexBiomeSource.hexSettings();
        this.hexBorderSurfaceBuilder = hexSettings.borderExtendsToBedrock() ? new HexBorderSurfaceBuilder(hexSettings.borderState()) : SurfaceBuilder.DEFAULT;
        this.hexBorderSurfaceBuilderConfig = new SurfaceBuilderConfig(hexSettings.borderState(), hexSettings.borderState(), hexSettings.borderState());

        final DimensionSettings dimensionSettings = settings.get();
        final NoiseSettings noiseSettings = dimensionSettings.noiseSettings();

        this.height = noiseSettings.height();
        this.chunkHeight = noiseSettings.noiseSizeVertical() * 4;
        this.chunkWidth = noiseSettings.noiseSizeHorizontal() * 4;
        this.defaultBlock = dimensionSettings.getDefaultBlock();
        this.defaultFluid = dimensionSettings.getDefaultFluid();
        this.chunkCountX = 16 / chunkWidth;
        this.chunkCountY = height / chunkHeight;
        this.chunkCountZ = 16 / chunkWidth;

        final SharedSeedRandom random = new SharedSeedRandom(seed);

        this.minLimitPerlinNoise = new OctavesNoiseGenerator(random, IntStream.rangeClosed(-15, 0));
        this.maxLimitPerlinNoise = new OctavesNoiseGenerator(random, IntStream.rangeClosed(-15, 0));
        this.mainPerlinNoise = new OctavesNoiseGenerator(random, IntStream.rangeClosed(-7, 0));
        this.surfaceNoise = noiseSettings.useSimplexSurfaceNoise() ? new PerlinNoiseGenerator(random, IntStream.rangeClosed(-3, 0)) : new OctavesNoiseGenerator(random, IntStream.rangeClosed(-3, 0));
        this.depthNoise = new OctavesNoiseGenerator(random, IntStream.rangeClosed(-15, 0));
    }

    @Override
    protected Codec<HexChunkGenerator> codec()
    {
        return CODEC;
    }

    @Override
    public ChunkGenerator withSeed(long seed)
    {
        return new HexChunkGenerator(hexBiomeSource.withSeed(seed), settings, seed);
    }

    @Override
    public void buildSurfaceAndBedrock(WorldGenRegion world, IChunk chunkIn)
    {
        final SharedSeedRandom random = new SharedSeedRandom(seed);
        random.setBaseChunkSeed(chunkIn.getPos().x, chunkIn.getPos().z);

        final ChunkPos chunkPos = chunkIn.getPos();
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

                final int y = chunkIn.getHeight(Heightmap.Type.WORLD_SURFACE_WG, localX, localZ) + 1;
                final double noise = surfaceNoise.getSurfaceNoiseValue(x * 0.0625D, z * 0.0625D, 0.0625D, localX * 0.0625D) * 15.0D;
                final Biome biome = biomesByHex.computeIfAbsent(hex, hexBiomeSource::getHexBiome);
                if (hex.radius(x * hexScale, z * hexScale) >= hexBorder)
                {
                    final Biome adjacentBiome = biomesByHex.computeIfAbsent(adjacentHex, hexBiomeSource::getHexBiome);
                    if (adjacentBiome != biome)
                    {
                        hexBorderSurfaceBuilder.apply(random, chunkIn, biome, x, z, y, noise, defaultBlock, defaultFluid, getSeaLevel(), world.getSeed(), hexBorderSurfaceBuilderConfig);
                        continue;
                    }
                }
                // Otherwise
                biome.buildSurfaceAt(random, chunkIn, x, z, y, noise, defaultBlock, defaultFluid, getSeaLevel(), world.getSeed());
            }
        }

        buildBedrock(chunkIn, random);
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion world)
    {
        // mojang are you serious? this is protected!?
        // if (!settings.get().disableMobGeneration())
        {
            final int centerX = world.getCenterX();
            final int centerZ = world.getCenterZ();
            final Biome biome = world.getBiome(new ChunkPos(centerX, centerZ).getWorldPosition());
            final SharedSeedRandom random = new SharedSeedRandom();
            random.setDecorationSeed(world.getSeed(), centerX << 4, centerZ << 4);
            WorldEntitySpawner.spawnMobsForChunkGeneration(world, biome, centerX, centerZ, random);
        }
    }

    public int getGenDepth()
    {
        return height;
    }

    @Override
    public List<MobSpawnInfo.Spawners> getMobsAt(Biome biome, StructureManager structureManager, EntityClassification entityClassification, BlockPos pos)
    {
        List<MobSpawnInfo.Spawners> spawns = StructureSpawnManager.getStructureSpawns(structureManager, entityClassification, pos);
        if (spawns != null)
        {
            return spawns; // thanks forge
        }
        return super.getMobsAt(biome, structureManager, entityClassification, pos);
    }

    @Override
    public void fillFromNoise(IWorld world, StructureManager structureManager, IChunk chunkIn)
    {
        final ChunkPos chunkPos = chunkIn.getPos();
        final int chunkX = chunkPos.x, chunkZ = chunkPos.z;
        final int blockX = chunkPos.getMinBlockX(), blockZ = chunkPos.getMinBlockZ();
        final Hex[] hexes = new Hex[16 * 16], adjacentHexes = new Hex[16 * 16];
        final Map<Hex, Biome> biomesByHex = new HashMap<>();

        final double hexScale = hexSettings.biomeScale();
        final double hexSize = hexSettings.hexSize() * hexScale;
        final double hexBorder = hexSettings.hexBorderThreshold();

        // Iterate each chunk position and identify the containing hex, and adjacent hex
        for (int x = 0; x < 16; x++)
        {
            for (int z = 0; z < 16; z++)
            {
                final int hx = blockX + x, hz = blockZ + z;
                final Hex hex = Hex.blockToHex(hx * hexScale, hz * hexScale, hexSize);
                final Hex adjacent = hex.adjacent(hx * hexScale, hz * hexScale);

                hexes[x + 16 * z] = hex;
                adjacentHexes[x + 16 * z] = adjacent;

                // Sample biomes once per each hex we encounter in the main grid.
                biomesByHex.computeIfAbsent(hex, hexBiomeSource::getHexBiome);
                biomesByHex.computeIfAbsent(adjacent, hexBiomeSource::getHexBiome);
            }
        }

        final ObjectList<StructurePiece> pieces = new ObjectArrayList<>(10);
        final ObjectList<JigsawJunction> junctions = new ObjectArrayList<>(32);
        Beardifier.sampleStructureContributions(chunkIn, structureManager, pieces, junctions);

        final double[][][] noiseValues = new double[2][chunkCountZ + 1][chunkCountY + 1];
        for (int z = 0; z < chunkCountZ + 1; ++z)
        {
            noiseValues[0][z] = new double[chunkCountY + 1];
            fillNoiseColumn(noiseValues[0][z], chunkX * chunkCountX, chunkZ * chunkCountZ + z);
            noiseValues[1][z] = new double[chunkCountY + 1];
        }

        final ChunkPrimer chunk = (ChunkPrimer) chunkIn;
        final Heightmap oceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Type.OCEAN_FLOOR_WG);
        final Heightmap worldSurface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Type.WORLD_SURFACE_WG);
        final BlockPos.Mutable pos = new BlockPos.Mutable();
        final Random hexBorderRandom = new Random();
        final ObjectListIterator<StructurePiece> pieceIterator = pieces.iterator();
        final ObjectListIterator<JigsawJunction> junctionIterator = junctions.iterator();

        for (int cellX = 0; cellX < chunkCountX; ++cellX)
        {
            for (int cellZ = 0; cellZ < chunkCountZ + 1; ++cellZ)
            {
                fillNoiseColumn(noiseValues[1][cellZ], chunkX * chunkCountX + cellX + 1, chunkZ * chunkCountZ + cellZ);
            }

            for (int cellZ = 0; cellZ < chunkCountZ; ++cellZ)
            {
                ChunkSection section = chunk.getOrCreateSection(15);
                section.acquire();

                for (int cellY = chunkCountY - 1; cellY >= 0; --cellY)
                {
                    /// noiseXYZ
                    final double noise000 = noiseValues[0][cellZ][cellY];
                    final double noise001 = noiseValues[0][cellZ + 1][cellY];
                    final double noise100 = noiseValues[1][cellZ][cellY];
                    final double noise101 = noiseValues[1][cellZ + 1][cellY];
                    final double noise010 = noiseValues[0][cellZ][cellY + 1];
                    final double noise011 = noiseValues[0][cellZ + 1][cellY + 1];
                    final double noise110 = noiseValues[1][cellZ][cellY + 1];
                    final double noise111 = noiseValues[1][cellZ + 1][cellY + 1];

                    for (int localY = chunkHeight - 1; localY >= 0; --localY)
                    {
                        final int y = cellY * chunkHeight + localY;
                        final int sectionY = y & 15;
                        final int sectionIndex = y >> 4;
                        if (section.bottomBlockY() >> 4 != sectionIndex)
                        {
                            section.release();
                            section = chunk.getOrCreateSection(sectionIndex);
                            section.acquire();
                        }

                        final double deltaY = (double) localY / chunkHeight;

                        // noiseXZ
                        final double noise00 = MathHelper.lerp(deltaY, noise000, noise010);
                        final double noise10 = MathHelper.lerp(deltaY, noise100, noise110);
                        final double noise01 = MathHelper.lerp(deltaY, noise001, noise011);
                        final double noise11 = MathHelper.lerp(deltaY, noise101, noise111);

                        for (int localX = 0; localX < chunkWidth; ++localX)
                        {
                            final int x = blockX + cellX * chunkWidth + localX;
                            final int sectionX = x & 15;
                            final double deltaX = (double) localX / chunkWidth;

                            // noiseZ
                            final double noise0 = MathHelper.lerp(deltaX, noise00, noise10);
                            final double noise1 = MathHelper.lerp(deltaX, noise01, noise11);

                            for (int localZ = 0; localZ < chunkWidth; ++localZ)
                            {
                                final int z = blockZ + cellZ * chunkWidth + localZ;
                                final int sectionZ = z & 15;
                                final double deltaZ = (double) localZ / chunkWidth;

                                final double noise = MathHelper.lerp(deltaZ, noise0, noise1);
                                double clampedNoise = MathHelper.clamp(noise / 200.0D, -1.0D, 1.0D);

                                // More beardifier nonsense
                                int x0, y0, z0;
                                for (clampedNoise = clampedNoise / 2.0D - clampedNoise * clampedNoise * clampedNoise / 24.0D; pieceIterator.hasNext(); clampedNoise += Beardifier.getContribution(x0, y0, z0) * 0.8D)
                                {
                                    StructurePiece piece = pieceIterator.next();
                                    MutableBoundingBox box = piece.getBoundingBox();
                                    x0 = Math.max(0, Math.max(box.x0 - x, x - box.x1));
                                    y0 = y - (box.y0 + (piece instanceof AbstractVillagePiece ? ((AbstractVillagePiece) piece).getGroundLevelDelta() : 0));
                                    z0 = Math.max(0, Math.max(box.z0 - z, z - box.z1));
                                }

                                pieceIterator.back(pieces.size());
                                while (junctionIterator.hasNext())
                                {
                                    JigsawJunction junction = junctionIterator.next();
                                    int dx = x - junction.getSourceX();
                                    x0 = y - junction.getSourceGroundY();
                                    y0 = z - junction.getSourceZ();
                                    clampedNoise += Beardifier.getContribution(dx, x0, y0) * 0.4D;
                                }
                                junctionIterator.back(junctions.size());

                                // Hex borders
                                final Hex hex = hexes[sectionX + sectionZ * 16];
                                if (hex.radius(x * hexScale, z * hexScale) > hexBorder)
                                {
                                    final Biome biome = biomesByHex.get(hex);
                                    final Hex adjacentHex = adjacentHexes[sectionX + sectionZ * 16];
                                    final Biome adjacentBiome = biomesByHex.get(adjacentHex);

                                    assert biome != null;
                                    assert adjacentBiome != null;

                                    if (biome != adjacentBiome) // No borders between adjacent hexes of the same biome
                                    {
                                        clampedNoise = applyHexBorderNoiseModifier(hex, biome, hexBorderRandom, y);
                                    }
                                }

                                final BlockState state = generateBaseState(clampedNoise, y);
                                if (!state.isAir())
                                {
                                    pos.set(x, y, z);
                                    if (state.getLightValue(chunk, pos) != 0)
                                    {
                                        chunk.addLight(pos);
                                    }

                                    section.setBlockState(sectionX, sectionY, sectionZ, state, false);
                                    oceanFloor.update(sectionX, y, sectionZ, state);
                                    worldSurface.update(sectionX, y, sectionZ, state);
                                }
                            }
                        }
                    }
                }

                section.release();
            }

            // Swap
            final double[][] noiseColumn = noiseValues[0];
            noiseValues[0] = noiseValues[1];
            noiseValues[1] = noiseColumn;
        }
    }

    @Override
    public int getSeaLevel()
    {
        return settings().seaLevel();
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Type type)
    {
        return iterateNoiseColumn(x, z, null, type.isOpaque());
    }

    @Override
    public IBlockReader getBaseColumn(int x, int z)
    {
        final BlockState[] column = new BlockState[chunkCountY * chunkHeight];
        iterateNoiseColumn(x, z, column, null);
        return new Blockreader(column);
    }

    protected DimensionSettings settings()
    {
        if (cachedSettings == null)
        {
            cachedSettings = settings.get();
        }
        return cachedSettings;
    }

    protected double applyHexBorderNoiseModifier(Hex hex, Biome biome, Random random, int y)
    {
        // Really really hacky way to get some sensible variety between hex border heights.
        random.setSeed(HashCommon.murmurHash3(hex.hashCode()));
        if (hexSettings.windowedBorder())
        {
            final int roof = height + 1 - settings().getBedrockRoofPosition() - 30 - random.nextInt(10) - random.nextInt(10);
            final int floor = settings().getBedrockFloorPosition() + 30 + random.nextInt(5) + random.nextInt(5);
            return y < floor || y > roof ? 1 : -1;
        }
        else
        {
            final double biomeHeight = getBaseBiomeHeight(biome) * chunkHeight;
            final int adjustedHeight = (int) (biomeHeight + random.nextInt(4) - random.nextInt(4));
            return y < adjustedHeight ? 1 : -1;
        }
    }

    protected BlockState generateBaseState(double noise, int y)
    {
        if (noise > 0)
        {
            return defaultBlock;
        }
        else if (y < getSeaLevel())
        {
            return defaultFluid;
        }
        else
        {
            return AIR;
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
        return (1 + (depth + densityOffset) / densityFactor) * (chunkCountY / 2.0D);
    }

    protected void fillNoiseColumn(double[] noiseColumn, int cellX, int cellZ)
    {
        final NoiseSettings noiseSettings = settings().noiseSettings();
        final Biome hexBiome = hexBiomeSource.getNoiseBiome(cellX, getSeaLevel(), cellZ);

        final double xzScale = 684.412D * noiseSettings.noiseSamplingSettings().xzScale();
        final double yScale = 684.412D * noiseSettings.noiseSamplingSettings().yScale();
        final double xzFactor = xzScale / noiseSettings.noiseSamplingSettings().xzFactor();
        final double yFactor = yScale / noiseSettings.noiseSamplingSettings().yFactor();
        final double topSlideTarget = noiseSettings.topSlideSettings().target();
        final double topSlideSize = noiseSettings.topSlideSettings().size();
        final double topSlideOffset = noiseSettings.topSlideSettings().offset();
        final double bottomSlideTarget = noiseSettings.bottomSlideSettings().target();
        final double bottomSlideSize = noiseSettings.bottomSlideSettings().size();
        final double bottomSlideOffset = noiseSettings.bottomSlideSettings().offset();
        final double randomDensityOffset = noiseSettings.randomDensityOffset() ? getRandomDensity(cellX, cellZ) : 0.0D;
        final double densityFactor = noiseSettings.densityFactor();
        final double densityOffset = noiseSettings.densityOffset();

        double scale = hexBiome.getScale();
        double depth = hexBiome.getDepth();
        if (noiseSettings.isAmplified() && depth > 0.0F)
        {
            depth = 1.0F + depth * 2.0F;
            scale = 1.0F + scale * 4.0F;
        }

        depth = (depth * 0.5F - 0.125F) * 0.265625D;
        scale = 96.0D / (scale * 0.9F + 0.1F);

        for (int cellY = 0; cellY <= chunkCountY; ++cellY)
        {
            double noise0 = sampleAndClampNoise(cellX, cellY, cellZ, xzScale, yScale, xzFactor, yFactor);
            double noise1 = 1.0D - cellY * 2.0D / chunkCountY + randomDensityOffset;
            double noise2 = noise1 * densityFactor + densityOffset;
            double noise3 = (noise2 + depth) * scale;
            if (noise3 > 0.0D)
            {
                noise0 = noise0 + noise3 * 4.0D;
            }
            else
            {
                noise0 = noise0 + noise3;
            }

            if (topSlideSize > 0.0D)
            {
                double topSlide = ((double) (chunkCountY - cellY) - topSlideOffset) / topSlideSize;
                noise0 = MathHelper.clampedLerp(topSlideTarget, noise0, topSlide);
            }

            if (bottomSlideSize > 0.0D)
            {
                double bottomSlide = ((double) cellY - bottomSlideOffset) / bottomSlideSize;
                noise0 = MathHelper.clampedLerp(bottomSlideTarget, noise0, bottomSlide);
            }

            noiseColumn[cellY] = noise0;
        }
    }

    protected double[] makeAndFillNoiseColumn(int x, int z)
    {
        final double[] noiseValues = new double[chunkCountY + 1];
        fillNoiseColumn(noiseValues, x, z);
        return noiseValues;
    }

    protected int iterateNoiseColumn(int x, int z, @Nullable BlockState[] column, @Nullable Predicate<BlockState> check)
    {
        int cellX = Math.floorDiv(x, chunkWidth);
        int cellZ = Math.floorDiv(z, chunkWidth);
        int localX = Math.floorMod(x, chunkWidth);
        int localZ = Math.floorMod(z, chunkWidth);
        double deltaX = (double) localX / chunkWidth;
        double deltaZ = (double) localZ / chunkWidth;
        double[][] noiseValues = new double[][] {makeAndFillNoiseColumn(cellX, cellZ), makeAndFillNoiseColumn(cellX, cellZ + 1), makeAndFillNoiseColumn(cellX + 1, cellZ), makeAndFillNoiseColumn(cellX + 1, cellZ + 1)};

        for (int cellY = chunkCountY - 1; cellY >= 0; --cellY)
        {
            double noise000 = noiseValues[0][cellY];
            double noise001 = noiseValues[1][cellY];
            double noise100 = noiseValues[2][cellY];
            double noise101 = noiseValues[3][cellY];
            double noise010 = noiseValues[0][cellY + 1];
            double noise011 = noiseValues[1][cellY + 1];
            double noise110 = noiseValues[2][cellY + 1];
            double noise111 = noiseValues[3][cellY + 1];

            for (int localY = chunkHeight - 1; localY >= 0; --localY)
            {
                double deltaY = (double) localY / chunkHeight;
                double noise = MathHelper.lerp3(deltaY, deltaX, deltaZ, noise000, noise010, noise100, noise110, noise001, noise011, noise101, noise111);
                int y = cellY * chunkHeight + localY;
                BlockState state = generateBaseState(noise, y);
                if (column != null)
                {
                    column[y] = state;
                }

                if (check != null && check.test(state))
                {
                    return y + 1;
                }
            }
        }
        return 0;
    }

    protected void buildBedrock(IChunk chunkIn, Random random)
    {
        final BlockPos.Mutable mutablePos = new BlockPos.Mutable();
        final int blockX = chunkIn.getPos().getMinBlockX();
        final int blockZ = chunkIn.getPos().getMinBlockZ();
        final DimensionSettings settings = settings();
        final int floorY = settings.getBedrockFloorPosition();
        final int roofY = height - 1 - settings.getBedrockRoofPosition();
        final boolean roof = roofY + 4 >= 0 && roofY < height;
        final boolean floor = floorY + 4 >= 0 && floorY < height;
        if (roof || floor)
        {
            for (BlockPos pos : BlockPos.betweenClosed(blockX, 0, blockZ, blockX + 15, 0, blockZ + 15))
            {
                if (roof)
                {
                    for (int y = 0; y < 5; ++y)
                    {
                        if (y <= random.nextInt(5))
                        {
                            chunkIn.setBlockState(mutablePos.set(pos.getX(), roofY - y, pos.getZ()), Blocks.BEDROCK.defaultBlockState(), false);
                        }
                    }
                }

                if (floor)
                {
                    for (int y = 4; y >= 0; --y)
                    {
                        if (y <= random.nextInt(5))
                        {
                            chunkIn.setBlockState(mutablePos.set(pos.getX(), floorY + y, pos.getZ()), Blocks.BEDROCK.defaultBlockState(), false);
                        }
                    }
                }
            }

        }
    }

    protected double sampleAndClampNoise(int cellX, int cellY, int cellZ, double xzScale, double yScale, double xzFactor, double yFactor)
    {
        double minLimit = 0.0D;
        double maxLimit = 0.0D;
        double main = 0.0D;
        double amplitude = 1.0D;

        for (int i = 0; i < 16; ++i)
        {
            double x0 = OctavesNoiseGenerator.wrap(cellX * xzScale * amplitude);
            double y0 = OctavesNoiseGenerator.wrap(cellY * yScale * amplitude);
            double z0 = OctavesNoiseGenerator.wrap(cellZ * xzScale * amplitude);
            double y1 = yScale * amplitude;
            ImprovedNoiseGenerator minLimitNoise = minLimitPerlinNoise.getOctaveNoise(i);
            if (minLimitNoise != null)
            {
                minLimit += minLimitNoise.noise(x0, y0, z0, y1, cellY * y1) / amplitude;
            }

            ImprovedNoiseGenerator maxLimitNoise = maxLimitPerlinNoise.getOctaveNoise(i);
            if (maxLimitNoise != null)
            {
                maxLimit += maxLimitNoise.noise(x0, y0, z0, y1, cellY * y1) / amplitude;
            }

            if (i < 8)
            {
                ImprovedNoiseGenerator mainNoise = mainPerlinNoise.getOctaveNoise(i);
                if (mainNoise != null)
                {
                    main += mainNoise.noise(OctavesNoiseGenerator.wrap(cellX * xzFactor * amplitude), OctavesNoiseGenerator.wrap(cellY * yFactor * amplitude), OctavesNoiseGenerator.wrap(cellZ * xzFactor * amplitude), yFactor * amplitude, cellY * yFactor * amplitude) / amplitude;
                }
            }
            amplitude /= 2.0D;
        }
        return MathHelper.clampedLerp(minLimit / 512.0D, maxLimit / 512.0D, (main / 10.0D + 1.0D) / 2.0D);
    }

    protected double getRandomDensity(int x, int z)
    {
        double noise0 = depthNoise.getValue(x * 200, 10.0D, z * 200, 1.0D, 0.0D, true);
        double noise1;
        if (noise0 < 0.0D)
        {
            noise1 = -noise0 * 0.3D;
        }
        else
        {
            noise1 = noise0;
        }

        double noise2 = noise1 * 24.575625D - 2.0D;
        return noise2 < 0.0D ? noise2 * 0.009486607142857142D : Math.min(noise2, 1.0D) * 0.006640625D;
    }
}
