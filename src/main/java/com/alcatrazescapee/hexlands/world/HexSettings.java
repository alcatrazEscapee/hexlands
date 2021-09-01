/*
 * Part of the HexLands mod.
 * Licensed under MIT. See the project LICENSE.txt for details.
 */

package com.alcatrazescapee.hexlands.world;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class HexSettings
{
    public static final MapCodec<HexSettings> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.doubleRange(0.01, 1000).optionalFieldOf("biome_scale", 8d).forGetter(c -> c.biomeScale),
        Codec.doubleRange(1, 1000).optionalFieldOf("hex_size", 40d).forGetter(c -> c.hexSize),
        Codec.doubleRange(0, 1).optionalFieldOf("hex_border_threshold", 0.92d).forGetter(c -> c.hexBorderThreshold),
        BlockState.CODEC.optionalFieldOf("border_state", Blocks.AIR.defaultBlockState()).forGetter(c -> c.borderState),
        Codec.BOOL.optionalFieldOf("border_extends_to_bedrock", false).forGetter(c -> c.borderExtendsToBedrock),
        Codec.BOOL.optionalFieldOf("windowed_border", false).forGetter(c -> c.windowedBorder),
        Codec.BOOL.optionalFieldOf("no_border", false).forGetter(c -> c.noBorder)
    ).apply(instance, HexSettings::new));

    public static final HexSettings OVERWORLD = new HexSettings(8d, 40d, 0.92d, Blocks.STONE_BRICKS.defaultBlockState(), false, false, false);
    public static final HexSettings NETHER = new HexSettings(4d, 40d, 0.92d, Blocks.NETHER_BRICKS.defaultBlockState(), true, true, false);
    public static final HexSettings END = new HexSettings(4d, 40d, 0.92d, Blocks.AIR.defaultBlockState(), true, false, true);

    private final double biomeScale;
    private final double hexSize;
    private final double hexBorderThreshold;
    private final BlockState borderState;
    private final boolean borderExtendsToBedrock;
    private final boolean windowedBorder;
    private final boolean noBorder;

    private HexSettings(double biomeScale, double hexSize, double hexBorderThreshold, BlockState borderState, boolean borderExtendsToBedrock, boolean windowedBorder, boolean noBorder)
    {
        this.biomeScale = biomeScale;
        this.hexSize = hexSize;
        this.hexBorderThreshold = hexBorderThreshold;
        this.borderState = borderState;
        this.borderExtendsToBedrock = borderExtendsToBedrock;
        this.windowedBorder = windowedBorder;
        this.noBorder = noBorder;
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

    public BlockState borderState()
    {
        return borderState;
    }

    public boolean borderExtendsToBedrock()
    {
        return borderExtendsToBedrock;
    }

    public boolean windowedBorder()
    {
        return windowedBorder;
    }

    public boolean noBorder()
    {
        return noBorder;
    }
}
