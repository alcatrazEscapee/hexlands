package com.alcatrazescapee.hexlands.util;

import java.util.Map;
import java.util.Optional;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.DataResult;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record HexSettings(double biomeScale, double hexSize, double hexBorderThreshold, Optional<BorderSettings> topBorder, Optional<BorderSettings> bottomBorder)
{
    private static final Map<ResourceLocation, HexSettings> DEFAULTS = new Object2ObjectOpenHashMap<>();
    private static final Codec<HexSettings> OBJECT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.doubleRange(0.01, 1000).optionalFieldOf("biome_scale", 8d).forGetter(c -> c.biomeScale),
            Codec.doubleRange(1, 1000).optionalFieldOf("hex_size", 40d).forGetter(c -> c.hexSize),
            Codec.doubleRange(0, 1).optionalFieldOf("hex_border_threshold", 0.92d).forGetter(c -> c.hexBorderThreshold),
            BorderSettings.CODEC.optionalFieldOf("top_border").forGetter(c -> c.topBorder),
            BorderSettings.CODEC.optionalFieldOf("bottom_border").forGetter(c -> c.bottomBorder)
    ).apply(instance, HexSettings::new));


    public static final HexSettings OVERWORLD = new HexSettings(32d, 40d, 0.92d, Optional.empty(), BorderSettings.of(62, 66, Blocks.STONE_BRICKS));
    public static final HexSettings NETHER = new HexSettings(4d, 40d, 0.92d, BorderSettings.of(100, 110, Blocks.NETHER_BRICKS), BorderSettings.of(31, 40, Blocks.NETHER_BRICKS));
    public static final HexSettings END = new HexSettings(4d, 40d, 0.92d, Optional.empty(), Optional.empty());

    public static final Codec<HexSettings> CODEC = Codec.either(ResourceLocation.CODEC, OBJECT_CODEC).flatXmap(x -> {
        var settings = x.map(s -> HexSettings.DEFAULTS.get(s), s -> s);
        if (settings == null)
        {
            return DataResult.error("Invalid hex settings resource location!");
        }
        return DataResult.success(settings);
    }, x -> DataResult.success(Either.right(x)));

    public record BorderSettings(int minHeight, int maxHeight, BlockState state)
    {
        public static final Codec<BorderSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("min_height").forGetter(c -> c.minHeight),
            Codec.INT.fieldOf("max_height").forGetter(c -> c.maxHeight),
            BlockState.CODEC.fieldOf("state").forGetter(c -> c.state)
        ).apply(instance, BorderSettings::new));

        private static Optional<BorderSettings> of(int minHeight, int maxHeight, Block block)
        {
            return Optional.of(new BorderSettings(minHeight, maxHeight, block.defaultBlockState()));
        }

        public int sample(RandomSource random)
        {
            if (minHeight == maxHeight)
            {
                return minHeight;
            }
            return random.nextIntBetweenInclusive(minHeight, maxHeight);
        }
    }

    static
    {
        DEFAULTS.put(new ResourceLocation("overworld"), OVERWORLD);
        DEFAULTS.put(new ResourceLocation("nether"), NETHER);
        DEFAULTS.put(new ResourceLocation("the_end"), END);
    }
}
