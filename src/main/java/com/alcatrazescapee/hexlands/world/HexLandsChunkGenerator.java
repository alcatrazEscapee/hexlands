package com.alcatrazescapee.hexlands.world;

import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import javax.annotation.Nullable;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityClassification;
import net.minecraft.util.SharedSeedRandom;
import net.minecraft.util.math.*;
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
import net.minecraft.world.gen.feature.jigsaw.JigsawPattern;
import net.minecraft.world.gen.feature.structure.AbstractVillagePiece;
import net.minecraft.world.gen.feature.structure.Structure;
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
public class HexLandsChunkGenerator extends ChunkGenerator
{
    public static final Codec<HexLandsChunkGenerator> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        BiomeProvider.CODEC.comapFlatMap(
            source -> {
                if (source instanceof HexLandsBiomeSource)
                {
                    return DataResult.success((HexLandsBiomeSource) source);
                }
                return DataResult.error("HexLands chunk generator requires a HexLands biome source");
            },
            source -> source
        ).fieldOf("biome_source").forGetter(c -> c.hexBiomeSource),
        DimensionSettings.CODEC.fieldOf("settings").forGetter(c -> c.settings),
        Codec.LONG.fieldOf("seed").forGetter(c -> c.seed)
    ).apply(instance, HexLandsChunkGenerator::new));

    public static final BlockState AIR = Blocks.AIR.defaultBlockState();
    public static final SurfaceBuilderConfig BORDER_CONFIG = new SurfaceBuilderConfig(Blocks.STONE_BRICKS.defaultBlockState(), Blocks.STONE_BRICKS.defaultBlockState(), Blocks.STONE_BRICKS.defaultBlockState());

    private final HexLandsBiomeSource hexBiomeSource;
    private final Supplier<DimensionSettings> settings;
    private final long seed;

    private final HexSettings hexSettings;

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

    public HexLandsChunkGenerator(HexLandsBiomeSource hexBiomeSource, Supplier<DimensionSettings> settings, long seed)
    {
        super(hexBiomeSource, settings.get().structureSettings());

        this.hexBiomeSource = hexBiomeSource;
        this.settings = settings;
        this.seed = seed;

        this.hexSettings = hexBiomeSource.hexSettings();

        DimensionSettings dimensionSettings = settings.get();
        NoiseSettings noiseSettings = dimensionSettings.noiseSettings();

        this.height = noiseSettings.height();
        this.chunkHeight = noiseSettings.noiseSizeVertical() * 4;
        this.chunkWidth = noiseSettings.noiseSizeHorizontal() * 4;
        this.defaultBlock = dimensionSettings.getDefaultBlock();
        this.defaultFluid = dimensionSettings.getDefaultFluid();
        this.chunkCountX = 16 / chunkWidth;
        this.chunkCountY = height / chunkHeight;
        this.chunkCountZ = 16 / chunkWidth;

        SharedSeedRandom random = new SharedSeedRandom(seed);

        this.minLimitPerlinNoise = new OctavesNoiseGenerator(random, IntStream.rangeClosed(-15, 0));
        this.maxLimitPerlinNoise = new OctavesNoiseGenerator(random, IntStream.rangeClosed(-15, 0));
        this.mainPerlinNoise = new OctavesNoiseGenerator(random, IntStream.rangeClosed(-7, 0));
        this.surfaceNoise = noiseSettings.useSimplexSurfaceNoise() ? new PerlinNoiseGenerator(random, IntStream.rangeClosed(-3, 0)) : new OctavesNoiseGenerator(random, IntStream.rangeClosed(-3, 0));
        this.depthNoise = new OctavesNoiseGenerator(random, IntStream.rangeClosed(-15, 0));
    }

    @Override
    protected Codec<HexLandsChunkGenerator> codec()
    {
        return CODEC;
    }

    @Override
    public ChunkGenerator withSeed(long seed)
    {
        return new HexLandsChunkGenerator(hexBiomeSource, settings, seed);
    }

    @Override
    public void buildSurfaceAndBedrock(WorldGenRegion world, IChunk chunkIn)
    {
        final SharedSeedRandom random = new SharedSeedRandom(seed);
        random.setBaseChunkSeed(chunkIn.getPos().x, chunkIn.getPos().z);

        final ChunkPos chunkPos = chunkIn.getPos();
        final int blockX = chunkPos.getMinBlockX();
        final int blockZ = chunkPos.getMinBlockZ();
        final BlockPos.Mutable pos = new BlockPos.Mutable();

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
                final int y = chunkIn.getHeight(Heightmap.Type.WORLD_SURFACE_WG, localX, localZ) + 1;
                final double surfaceNoiseValue = surfaceNoise.getSurfaceNoiseValue(x * 0.0625D, z * 0.0625D, 0.0625D, localX * 0.0625D) * 15.0D;
                final Biome biome = world.getBiome(pos.set(x, y, z));
                if (hex.radius(x * hexScale, z * hexScale) < hexBorder)
                {
                    biome.buildSurfaceAt(random, chunkIn, x, z, y, surfaceNoiseValue, defaultBlock, defaultFluid, getSeaLevel(), world.getSeed());
                }
                else
                {
                    SurfaceBuilder.DEFAULT.apply(random, chunkIn, biome, x, z, y, surfaceNoiseValue, defaultBlock, defaultFluid, getSeaLevel(), world.getSeed(), BORDER_CONFIG);
                }
            }
        }

        buildBedrock(chunkIn, random);
    }

    @Override
    public void fillFromNoise(IWorld world, StructureManager structureManager, IChunk chunkIn)
    {
        final int blockX = chunkIn.getPos().getMinBlockX(), blockZ = chunkIn.getPos().getMinBlockZ();
        final Hex[] hexes = new Hex[16 * 16];
        final Biome[] hexBiomes = new Biome[16 * 16];

        final double hexScale = hexSettings.biomeScale();
        final double hexSize = hexSettings.hexSize() * hexScale;
        final double hexBorder = hexSettings.hexBorderThreshold();

        for (int x = 0; x < 16; x++)
        {
            for (int z = 0; z < 16; z++)
            {
                final int hx = blockX + x, hz = blockZ + z;
                hexes[x + 16 * z] = Hex.blockToHex(hx * hexScale, hz * hexScale, hexSize);
                hexBiomes[x + 16 * z] = hexBiomeSource.getHexBiome(hx, hz);
            }
        }

        final ObjectList<StructurePiece> pieces = new ObjectArrayList<>(10);
        final ObjectList<JigsawJunction> junctions = new ObjectArrayList<>(32);
        final ChunkPos chunkpos = chunkIn.getPos();
        final int chunkX = chunkpos.x;
        final int chunkZ = chunkpos.z;
        final int bigChunkX = chunkX << 4;
        final int bigChunkZ = chunkZ << 4;

        for (Structure<?> structure : Structure.NOISE_AFFECTING_FEATURES)
        {
            structureManager.startsForFeature(SectionPos.of(chunkpos, 0), structure).forEach(start -> {
                for (StructurePiece piece : start.getPieces())
                {
                    if (piece.isCloseToChunk(chunkpos, 12))
                    {
                        if (piece instanceof AbstractVillagePiece)
                        {
                            AbstractVillagePiece villagePiece = (AbstractVillagePiece) piece;
                            JigsawPattern.PlacementBehaviour behavior = villagePiece.getElement().getProjection();
                            if (behavior == JigsawPattern.PlacementBehaviour.RIGID)
                            {
                                pieces.add(villagePiece);
                            }

                            for (JigsawJunction junction : villagePiece.getJunctions())
                            {
                                int l5 = junction.getSourceX();
                                int i6 = junction.getSourceZ();
                                if (l5 > bigChunkX - 12 && i6 > bigChunkZ - 12 && l5 < bigChunkX + 15 + 12 && i6 < bigChunkZ + 15 + 12)
                                {
                                    junctions.add(junction);
                                }
                            }
                        }
                        else
                        {
                            pieces.add(piece);
                        }
                    }
                }

            });
        }

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
                            final int x = bigChunkX + cellX * chunkWidth + localX;
                            final int sectionX = x & 15;
                            final double deltaX = (double) localX / chunkWidth;

                            // noiseZ
                            final double noise0 = MathHelper.lerp(deltaX, noise00, noise10);
                            final double noise1 = MathHelper.lerp(deltaX, noise01, noise11);

                            for (int localZ = 0; localZ < chunkWidth; ++localZ)
                            {
                                final int z = bigChunkZ + cellZ * chunkWidth + localZ;
                                final int sectionZ = z & 15;
                                final double deltaZ = (double) localZ / chunkWidth;

                                final double noise = MathHelper.lerp(deltaZ, noise0, noise1);
                                double clampedNoise = MathHelper.clamp(noise / 200.0D, -1.0D, 1.0D);

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

                                final Hex hex = hexes[sectionX + sectionZ * 16];
                                if (hex.radius(x * hexScale, z * hexScale) > hexBorder)
                                {
                                    // border between two hexes.
                                    final Biome biome = hexBiomes[sectionX + sectionZ * 16];
                                    // Really really hacky way to get some sensible variety between hex borders.
                                    double depth;
                                    switch (biome.getBiomeCategory()) {
                                        case OCEAN: depth = -14; break;
                                        case BEACH: depth = 2; break;
                                        case MESA: depth = 14; break;
                                        case RIVER: depth = -3; break;
                                        case SWAMP: depth = -1; break;
                                        case EXTREME_HILLS: depth = 20; break;
                                        default: depth = 4;
                                    }
                                    hexBorderRandom.setSeed(HashCommon.murmurHash3(hex.hashCode()));
                                    final int adjustedHeight = (int) (getSeaLevel() + depth + hexBorderRandom.nextInt(4) - hexBorderRandom.nextInt(4));
                                    clampedNoise = y < adjustedHeight ? 1 : -1;
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

    protected void fillNoiseColumn(double[] noiseColumn, int cellX, int cellZ)
    {
        NoiseSettings noiseSettings = settings.get().noiseSettings();
        Biome hexBiome = hexBiomeSource.getNoiseBiome(cellX, getSeaLevel(), cellZ);
        float scale = hexBiome.getScale();
        float depth = hexBiome.getDepth();

        if (noiseSettings.isAmplified() && depth > 0.0F)
        {
            depth = 1.0F + depth * 2.0F;
            scale = 1.0F + scale * 4.0F;
        }

        double depth0 = depth * 0.5F - 0.125F;
        double scale0 = scale * 0.9F + 0.1F;
        double depth1 = depth0 * 0.265625D;
        double scale1 = 96.0D / scale0;

        double xzScale = 684.412D * noiseSettings.noiseSamplingSettings().xzScale();
        double yScale = 684.412D * noiseSettings.noiseSamplingSettings().yScale();
        double xzFactor = xzScale / noiseSettings.noiseSamplingSettings().xzFactor();
        double yFactor = yScale / noiseSettings.noiseSamplingSettings().yFactor();
        double topSlideTarget = noiseSettings.topSlideSettings().target();
        double topSlideSize = noiseSettings.topSlideSettings().size();
        double topSlideOffset = noiseSettings.topSlideSettings().offset();
        double bottomSlideTarget = noiseSettings.bottomSlideSettings().target();
        double bottomSlideSize = noiseSettings.bottomSlideSettings().size();
        double bottomSlideOffset = noiseSettings.bottomSlideSettings().offset();
        double randomDensityOffset = noiseSettings.randomDensityOffset() ? this.getRandomDensity(cellX, cellZ) : 0.0D;
        double densityFactor = noiseSettings.densityFactor();
        double densityOffset = noiseSettings.densityOffset();

        for (int cellY = 0; cellY <= this.chunkCountY; ++cellY)
        {
            double noise0 = sampleAndClampNoise(cellX, cellY, cellZ, xzScale, yScale, xzFactor, yFactor);
            double noise1 = 1.0D - cellY * 2.0D / chunkCountY + randomDensityOffset;
            double noise2 = noise1 * densityFactor + densityOffset;
            double noise3 = (noise2 + depth1) * scale1;
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

    private double sampleAndClampNoise(int cellX, int cellY, int cellZ, double xzScale, double yScale, double xzFactor, double yFactor)
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

    private double getRandomDensity(int x, int z)
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

    public int getGenDepth() {
        return this.height;
    }

    @Override
    public int getSeaLevel()
    {
        if (cachedSettings == null)
        {
            cachedSettings = settings.get();
        }
        return cachedSettings.seaLevel();
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

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Type type) {
        return iterateNoiseColumn(x, z, null, type.isOpaque());
    }

    @Override
    public IBlockReader getBaseColumn(int x, int z) {
        final BlockState[] column = new BlockState[chunkCountY * chunkHeight];
        iterateNoiseColumn(x, z, column, null);
        return new Blockreader(column);
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
        final BlockPos.Mutable pos = new BlockPos.Mutable();
        final int chunkX = chunkIn.getPos().getMinBlockX();
        final int chunkZ = chunkIn.getPos().getMinBlockZ();
        for (int x = 0; x < 16; x++)
        {
            pos.setX(chunkX + x);
            for (int z = 0; z < 16; z++)
            {
                pos.setZ(chunkZ + z);
                for (int y = 4; y>= 0; y--)
                {
                    pos.setY(y);
                    if (y <= random.nextInt(5))
                    {
                        chunkIn.setBlockState(pos, Blocks.BEDROCK.defaultBlockState(), false);
                    }
                }
            }
        }
    }
}
