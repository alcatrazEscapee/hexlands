package com.alcatrazescapee.hexlands.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class HexSettings
{
    public static final MapCodec<HexSettings> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.doubleRange(0.01, 1000).fieldOf("biome_scale").forGetter(c -> c.biomeScale),
        Codec.doubleRange(1, 1000).fieldOf("hex_size").forGetter(c -> c.hexSize),
        Codec.doubleRange(0, 1).fieldOf("hex_border_threshold").forGetter(c -> c.hexBorderThreshold)
    ).apply(instance, HexSettings::new));

    private final double biomeScale;
    private final double hexSize;
    private final double hexBorderThreshold;

    public HexSettings()
    {
        this(8d, 40d, 0.92d);
    }

    public HexSettings(double biomeScale, double hexSize, double hexBorderThreshold)
    {
        this.biomeScale = biomeScale;
        this.hexSize = hexSize;
        this.hexBorderThreshold = hexBorderThreshold;
    }

    public double biomeScale()
    {
        return biomeScale;
    }

    public double hexSize()
    {
        return hexSize;
    }

    public double hexBorderThreshold()
    {
        return hexBorderThreshold;
    }
}
