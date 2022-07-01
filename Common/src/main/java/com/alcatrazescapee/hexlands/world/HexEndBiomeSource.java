/*
 * Part of the HexLands mod.
 * Licensed under MIT. See the project LICENSE.txt for details.
 */

package com.alcatrazescapee.hexlands.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.TheEndBiomeSource;

import com.alcatrazescapee.hexlands.util.Hex;
import com.alcatrazescapee.hexlands.util.HexSettings;

public class HexEndBiomeSource extends BiomeSource
{
    public static final Codec<HexEndBiomeSource> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        RegistryOps.retrieveRegistry(Registry.BIOME_REGISTRY).forGetter(c -> c.biomeRegistry),
        Codec.LONG.fieldOf("seed").forGetter(c -> c.seed)
    ).apply(instance, HexEndBiomeSource::new));

    private final TheEndBiomeSource parent;
    private final Registry<Biome> biomeRegistry;
    private final long seed;

    private HexSettings settings;
    private long endMainIslandRadius;

    public HexEndBiomeSource(Registry<Biome> biomeRegistry, long seed)
    {
        this(new TheEndBiomeSource(biomeRegistry, seed), biomeRegistry, seed);
    }

    private HexEndBiomeSource(TheEndBiomeSource parent, Registry<Biome> biomeRegistry, long seed)
    {
        super(parent.possibleBiomes().stream());

        this.biomeRegistry = biomeRegistry;
        this.seed = seed;

        this.parent = parent;
        this.settings = HexSettings.END;
        this.endMainIslandRadius = 4096L;
    }

    public void applySettings(HexSettings settings)
    {
        this.settings = settings;
        this.endMainIslandRadius = (long) (4096L * settings.biomeScale() * settings.biomeScale());
    }

    @Override
    protected Codec<? extends BiomeSource> codec()
    {
        return CODEC;
    }

    @Override
    public HexEndBiomeSource withSeed(long seed)
    {
        return new HexEndBiomeSource(biomeRegistry, seed);
    }

    @Override
    public Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ, Climate.Sampler sampler)
    {
        final long chunkX = QuartPos.toSection(quartX), chunkZ = QuartPos.toSection(quartZ);
        if (chunkX * chunkX + chunkZ * chunkZ < 4096L)
        {
            return parent.getNoiseBiome(0, 0, 0, sampler);
        }

        final long blockX = QuartPos.toBlock(quartX), blockZ = QuartPos.toBlock(quartZ);

        final double scale = settings.biomeScale();
        final double size = settings.hexSize() * scale;
        final Hex hex = Hex.blockToHex(blockX * scale, blockZ * scale, size);
        final BlockPos pos = hex.center();
        return parent.getNoiseBiome(pos.getX(), quartY, pos.getZ(), sampler);
    }
}
