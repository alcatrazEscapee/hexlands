package com.alcatrazescapee.hexlands;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;

import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(HexLands.MOD_ID)
public final class ForgeHexLands
{
    public ForgeHexLands()
    {
        HexLands.init();

        final IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        modBus.addListener(this::setup);
    }

    private void setup(FMLCommonSetupEvent event)
    {
        event.enqueueWork(() -> {
            HexLands.registerCodecs();
        });
    }
}