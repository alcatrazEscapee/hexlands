package com.alcatrazescapee.hexlands;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import com.alcatrazescapee.hexlands.util.HexLandsConfig;
import com.alcatrazescapee.hexlands.world.HexBiomeSource;
import com.alcatrazescapee.hexlands.world.HexChunkGenerator;
import com.alcatrazescapee.hexlands.world.HexLandsWorldType;

@Mod(HexLands.MOD_ID)
public class HexLands
{
    public static final String MOD_ID = "hexlands";

    private static final Logger LOGGER = LogManager.getLogger();

    public HexLands()
    {
        LOGGER.info("Wait, this isn't Catan...");

        final IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        HexLandsWorldType.WORLD_TYPES.register(bus);
        bus.addListener(this::setup);
        HexLandsConfig.init();
    }

    private void setup(FMLCommonSetupEvent event)
    {
        event.enqueueWork(() -> {
            HexLandsWorldType.setDefault();
            Registry.register(Registry.CHUNK_GENERATOR, new ResourceLocation(MOD_ID, "hexlands"), HexChunkGenerator.CODEC);
            Registry.register(Registry.BIOME_SOURCE, new ResourceLocation(MOD_ID, "hexlands"), HexBiomeSource.CODEC);
        });
    }
}
