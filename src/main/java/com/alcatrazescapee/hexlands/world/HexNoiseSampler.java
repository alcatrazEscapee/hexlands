/*
 * Part of the HexLands mod.
 * Licensed under MIT. See the project LICENSE.txt for details.
 */

package com.alcatrazescapee.hexlands.world;

import javax.annotation.Nullable;

import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.TheEndBiomeSource;
import net.minecraft.world.level.levelgen.NoiseModifier;
import net.minecraft.world.level.levelgen.NoiseSampler;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.synth.BlendedNoise;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

public class HexNoiseSampler extends NoiseSampler
{
    private final HexBiomeSource hexBiomeSource;

    public HexNoiseSampler(HexBiomeSource hexBiomeSource, int cellWidth, int cellHeight, int cellCountY, NoiseSettings noiseSettings, BlendedNoise blendedNoise, @Nullable SimplexNoise islandNoise, PerlinNoise depthNoise, NoiseModifier caveNoiseModifier)
    {
        super(hexBiomeSource, cellWidth, cellHeight, cellCountY, noiseSettings, blendedNoise, islandNoise, depthNoise, caveNoiseModifier);

        this.hexBiomeSource = hexBiomeSource;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void fillNoiseColumn(double[] column, int cellX, int cellZ, NoiseSettings settings, int seaLevel, int minCellY, int cellCountY)
    {
        double depth, scale;
        if (islandNoise != null)
        {
            depth = TheEndBiomeSource.getHeightValue(islandNoise, cellX, cellZ) - 8.0F;
            if (depth > 0.0D)
            {
                scale = 0.25D;
            }
            else
            {
                scale = 1.0D;
            }
        }
        else
        {
            final Biome hexBiome = hexBiomeSource.getNoiseBiome(cellX, seaLevel, cellZ);
            depth = hexBiome.getDepth();
            scale = hexBiome.getScale();
            if (settings.isAmplified() && depth > 0.0F)
            {
                depth = 1.0F + depth * 2.0F;
                scale = 1.0F + scale * 4.0F;
            }

            depth = (depth * 0.5F - 0.125F) * 0.265625D;
            scale = 96.0D / (scale * 0.9F + 0.1F);
        }

        double xzScale = 684.412D * settings.noiseSamplingSettings().xzScale();
        double yScale = 684.412D * settings.noiseSamplingSettings().yScale();
        double xzFactor = xzScale / settings.noiseSamplingSettings().xzFactor();
        double yFactor = yScale / settings.noiseSamplingSettings().yFactor();
        double randomDensity = settings.randomDensityOffset() ? getRandomDensity(cellX, cellZ) : 0.0D;

        for (int cellIndexY = 0; cellIndexY <= cellCountY; ++cellIndexY)
        {
            int cellY = cellIndexY + minCellY;
            double noise = blendedNoise.sampleAndClampNoise(cellX, cellY, cellZ, xzScale, yScale, xzFactor, yFactor);
            noise += computeInitialDensity(cellY, depth, scale, randomDensity);
            noise = caveNoiseModifier.modifyNoise(noise, cellY * cellHeight, cellZ * cellWidth, cellZ * cellWidth);
            noise = applySlide(noise, cellY);
            column[cellIndexY] = noise;
        }
    }
}
