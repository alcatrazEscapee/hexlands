package com.alcatrazescapee.hexlands.util;

import java.util.Map;
import java.util.Optional;

import com.alcatrazescapee.hexlands.HexLands;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.DataResult;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record HexSettings(double biomeScale, double hexSize, double hexBorderThreshold, Optional<BorderSettings> topBorder, Optional<BorderSettings> bottomBorder)
{
    private static final Map<ResourceLocation, HexSettings> DEFAULTS = new Object2ObjectOpenHashMap<>();
    private static final Codec<HexSettings> DIRECT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.doubleRange(0.01, 1000).optionalFieldOf("biome_scale", 8d).forGetter(c -> c.biomeScale),
            Codec.doubleRange(1, 1000).optionalFieldOf("hex_size", 40d).forGetter(c -> c.hexSize),
            Codec.doubleRange(0, 1).optionalFieldOf("hex_border_threshold", 0.92d).forGetter(c -> c.hexBorderThreshold),
            BorderSettings.CODEC.optionalFieldOf("top_border").forGetter(c -> c.topBorder),
            BorderSettings.CODEC.optionalFieldOf("bottom_border").forGetter(c -> c.bottomBorder)
    ).apply(instance, HexSettings::new));

    public static final Codec<HexSettings> CODEC = Codec.either(ResourceLocation.CODEC, DIRECT_CODEC).comapFlatMap(
        e -> e.map(
            l -> Optional.ofNullable(HexSettings.DEFAULTS.get(l))
                .map(DataResult::success)
                .orElseGet(() -> DataResult.error(() -> "No hex_settings named '" + l + "'")),
            DataResult::success),
        Either::right);

    static
    {
        register("overworld", new HexSettings(32d, 40d, 0.92d, Optional.empty(), BorderSettings.of(62, 66, Blocks.STONE_BRICKS)));
        register("nether", new HexSettings(4d, 40d, 0.92d, BorderSettings.of(100, 110, Blocks.NETHER_BRICKS), BorderSettings.of(31, 40, Blocks.NETHER_BRICKS)));
        register("the_end", new HexSettings(4d, 40d, 0.92d, Optional.empty(), Optional.empty()));
    }

    private static void register(String id, HexSettings settings)
    {
        DEFAULTS.put(ResourceLocation.fromNamespaceAndPath(HexLands.MOD_ID, id), settings);
    }

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
}
