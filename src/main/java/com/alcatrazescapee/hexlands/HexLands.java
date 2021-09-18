/*
 * Part of the HexLands mod.
 * Licensed under MIT. See the project LICENSE.txt for details.
 */

package com.alcatrazescapee.hexlands;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraft.core.Registry;
import net.minecraft.data.worldgen.Features;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.BiomeLoadingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import com.alcatrazescapee.hexlands.util.HexLandsConfig;
import com.alcatrazescapee.hexlands.world.HexBiomeSource;
import com.alcatrazescapee.hexlands.world.HexChunkGenerator;
import com.alcatrazescapee.hexlands.world.HexEndBiomeSource;
import com.alcatrazescapee.hexlands.world.HexLandsWorldType;

@Mod(HexLands.MOD_ID)
public class HexLands
{
    public static final String MOD_ID = "hexlands";

    private static final Logger LOGGER = LogManager.getLogger();

    public HexLands()
    {
        LOGGER.info("Wait, this isn't Catan...");

        final IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        final IEventBus forgeBus = MinecraftForge.EVENT_BUS;

        HexLandsWorldType.WORLD_TYPES.register(modBus);
        modBus.addListener(this::setup);
        forgeBus.addListener(EventPriority.HIGH, this::onBiomeLoad);

        HexLandsConfig.init();
    }

    private void setup(FMLCommonSetupEvent event)
    {
        event.enqueueWork(() -> {
            HexLandsWorldType.setDefault();
            Registry.register(Registry.CHUNK_GENERATOR, new ResourceLocation(MOD_ID, "hexlands"), HexChunkGenerator.CODEC);
            Registry.register(Registry.BIOME_SOURCE, new ResourceLocation(MOD_ID, "hexlands"), HexBiomeSource.CODEC);
            Registry.register(Registry.BIOME_SOURCE, new ResourceLocation(MOD_ID, "end_hexlands"), HexEndBiomeSource.CODEC);
        });
    }

    private void onBiomeLoad(BiomeLoadingEvent event)
    {
        if (HexLandsConfig.COMMON.addEndSpikesToEndBiomes.get() && event.getName() != null && event.getCategory() == Biome.BiomeCategory.THEEND && !Biomes.THE_END.location().equals(event.getName()))
        {
            event.getGeneration().addFeature(GenerationStep.Decoration.SURFACE_STRUCTURES, Features.END_SPIKE);
        }
    }
}
