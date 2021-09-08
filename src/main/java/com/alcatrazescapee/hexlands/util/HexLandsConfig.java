/*
 * Part of the HexLands mod.
 * Licensed under MIT. See the project LICENSE.txt for details.
 */

package com.alcatrazescapee.hexlands.util;

import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public class HexLandsConfig
{
    public static final Common COMMON = register(ModConfig.Type.COMMON, Common::new);

    public static void init() {}

    private static <C> C register(ModConfig.Type type, Function<ForgeConfigSpec.Builder, C> factory)
    {
        Pair<C, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(factory);
        ModLoadingContext.get().registerConfig(type, specPair.getRight());
        return specPair.getLeft();
    }

    public static class Common
    {
        public final ForgeConfigSpec.BooleanValue setHexLandsWorldTypeAsDefault;
        public final ForgeConfigSpec.BooleanValue preserveMainEndIsland;
        public final ForgeConfigSpec.BooleanValue addEndSpikesToEndBiomes;
        public final ForgeConfigSpec.BooleanValue useBoPOverworld;
        public final ForgeConfigSpec.BooleanValue useBoPNether;

        private Common(ForgeConfigSpec.Builder builder)
        {
            setHexLandsWorldTypeAsDefault = builder.comment(
                " Should HexLands try and set the 'hexlands:hexlands' world type as the default world type?",
                " This will only replace the option in the respective Forge config file, *only* if it is set to 'default'"
            ).define("setHexLandsWorldTypeAsDefault", true);

            preserveMainEndIsland = builder.comment(
                " Should HexLands try and preserve the main end island, including end pillars (spikes), but resulting in less or no hexagons near the end origin."
            ).define("preserveMainEndIsland", true);

            addEndSpikesToEndBiomes = builder.comment(
                " Should HexLands add the 'minecraft:end_spike' feature (end pillars, with a crystal on top) to all End biomes (biomes that define their category as 'the_end')",
                " When used together with preserveMainEndIsland = false, this will create a fully hexagonal End dimension, but end spikes will still generate in their normal positions."
            ).define("addEndSpikesToEndBiomes", false);

            useBoPOverworld = builder.comment(
                " Should HexLands try and use the BoP (Biomes O Plenty) biome source for the overworld?"
            ).define("useBoPOverworld", true);

            useBoPNether = builder.comment(
                " Should HexLands try and use the BoP (Biomes O Plenty) biome source for the nether?"
            ).define("useBoPNether", true);
        }
    }
}
