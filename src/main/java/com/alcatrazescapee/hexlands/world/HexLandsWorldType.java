package com.alcatrazescapee.hexlands.world;

import net.minecraft.world.biome.provider.BiomeProvider;
import net.minecraft.world.biome.provider.OverworldBiomeProvider;
import net.minecraft.world.gen.DimensionSettings;
import net.minecraftforge.common.ForgeConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.world.ForgeWorldType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import com.alcatrazescapee.hexlands.HexLands;
import com.alcatrazescapee.hexlands.util.HexLandsConfig;

public class HexLandsWorldType
{
    public static final DeferredRegister<ForgeWorldType> WORLD_TYPES = DeferredRegister.create(ForgeRegistries.WORLD_TYPES, HexLands.MOD_ID);

    public static final RegistryObject<ForgeWorldType> HEX_LANDS = WORLD_TYPES.register("hexlands", () -> new ForgeWorldType((biomeRegistry, dimensionSettingsRegistry, seed) -> {
        final HexSettings settings = new HexSettings();
        final BiomeProvider overworld = new OverworldBiomeProvider(seed, false, false, biomeRegistry);
        final HexLandsBiomeSource hexlands = new HexLandsBiomeSource(overworld, biomeRegistry, settings, seed);
        return new HexLandsChunkGenerator(hexlands, () -> dimensionSettingsRegistry.getOrThrow(DimensionSettings.OVERWORLD), seed);
    }));

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void setDefault()
    {
        if (HexLandsConfig.COMMON.setHexLandsWorldTypeAsDefault.get() && ForgeConfig.COMMON.defaultWorldType.get().equals("default"))
        {
            ((ForgeConfigSpec.ConfigValue) ForgeConfig.COMMON.defaultWorldType).set(HexLandsWorldType.HEX_LANDS.getId().toString());
        }
    }
}
