package com.alcatrazescapee.hexlands;

import static com.alcatrazescapee.hexlands.HexLands.MOD_ID;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

import com.alcatrazescapee.hexlands.world.HexChunkGenerator;
import com.alcatrazescapee.hexlands.world.HexLandsWorldType;

@Mod(MOD_ID)
public final class ForgeHexLands
{
    private static final Logger LOGGER = LogUtils.getLogger();

    public ForgeHexLands()
    {
        LOGGER.info("Wait, this isn't Catan...");

        final IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        final IEventBus forgeBus = MinecraftForge.EVENT_BUS;

        HexLandsWorldType.WORLD_TYPES.register(modBus);
        modBus.addListener(this::setup);
        //forgeBus.addListener(EventPriority.HIGH, this::onBiomeLoad);

        //HexLandsConfig.init();
    }

    private void setup(FMLCommonSetupEvent event)
    {
        event.enqueueWork(() -> {
            HexLandsWorldType.setDefault();
            Registry.register(Registry.CHUNK_GENERATOR, new ResourceLocation(MOD_ID, "hexlands"), HexChunkGenerator.CODEC);
            //Registry.register(Registry.BIOME_SOURCE, new ResourceLocation(MOD_ID, "hexlands"), HexBiomeSource.CODEC);
            //Registry.register(Registry.BIOME_SOURCE, new ResourceLocation(MOD_ID, "end_hexlands"), HexEndBiomeSource.CODEC);
        });
    }
}