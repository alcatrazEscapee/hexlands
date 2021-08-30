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
        Codec.doubleRange(0.01, 1000).fieldOf("biome_scale").forGetter(c -> c.biomeScale),
        Codec.doubleRange(1, 1000).fieldOf("hex_size").forGetter(c -> c.hexSize),
        Codec.doubleRange(0, 1).fieldOf("hex_border_threshold").forGetter(c -> c.hexBorderThreshold),
        BlockState.CODEC.fieldOf("border_state").forGetter(c -> c.borderState),
        Codec.BOOL.fieldOf("border_extends_to_bedrock").forGetter(c -> c.borderExtendsToBedrock)
    ).apply(instance, HexSettings::new));

    public static final HexSettings OVERWORLD = new HexSettings(8d, 40d, 0.92d, Blocks.STONE_BRICKS.defaultBlockState(), false);
    public static final HexSettings NETHER = new HexSettings(4d, 40d, 0.92d, Blocks.NETHER_BRICKS.defaultBlockState(), true);

    private final double biomeScale;
    private final double hexSize;
    private final double hexBorderThreshold;
    private final BlockState borderState;
    private final boolean borderExtendsToBedrock;

    private HexSettings(double biomeScale, double hexSize, double hexBorderThreshold, BlockState borderState, boolean borderExtendsToBedrock)
    {
        this.biomeScale = biomeScale;
        this.hexSize = hexSize;
        this.hexBorderThreshold = hexBorderThreshold;
        this.borderState = borderState;
        this.borderExtendsToBedrock = borderExtendsToBedrock;
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
}
