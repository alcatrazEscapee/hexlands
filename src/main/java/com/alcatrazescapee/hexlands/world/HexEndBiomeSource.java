/*
 * Part of the HexLands mod.
 * Licensed under MIT. See the project LICENSE.txt for details.
 */

package com.alcatrazescapee.hexlands.world;

import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryLookupCodec;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class HexEndBiomeSource extends HexBiomeSource
{
    public static final Codec<HexEndBiomeSource> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        BiomeSource.CODEC.fieldOf("biome_source").forGetter(c -> c.parent),
        RegistryLookupCodec.create(Registry.BIOME_REGISTRY).forGetter(c -> c.biomeRegistry),
        HexSettings.CODEC.forGetter(c -> c.settings)
    ).apply(instance, HexEndBiomeSource::new));

    private final long endMainIslandRadius;

    public HexEndBiomeSource(BiomeSource parent, Registry<Biome> biomeRegistry, HexSettings settings)
    {
        super(parent, biomeRegistry, settings);

        endMainIslandRadius = (long) (4096L * settings.biomeScale() * settings.biomeScale());
    }

    @Override
    protected Codec<? extends BiomeSource> codec()
    {
        return CODEC;
    }

    @Override
    public HexBiomeSource withSeed(long seed)
    {
        return new HexEndBiomeSource(parent.withSeed(seed), biomeRegistry, settings);
    }

    @Override
    protected Biome getParentNoiseBiome(int x, int y, int z)
    {
        final long chunkX = x >> 2, chunkZ = z >> 2;
        return chunkX * chunkX + chunkZ * chunkZ < endMainIslandRadius ? parent.getNoiseBiome(0, 0, 0) : parent.getNoiseBiome(x, y, z);
    }
}
