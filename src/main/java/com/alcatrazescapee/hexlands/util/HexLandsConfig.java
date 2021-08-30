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

        private Common(ForgeConfigSpec.Builder builder)
        {
            setHexLandsWorldTypeAsDefault = builder.comment(
                " Should HexLands try and set the 'hexlands:hexlands' world type as the default world type?",
                " This will only replace the option in the respective Forge config file, *only* if it is set to 'default'"
            ).define("setHexLandsWorldTypeAsDefault", true);
        }
    }
}
