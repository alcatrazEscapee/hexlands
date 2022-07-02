package com.alcatrazescapee.hexlands;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;

import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import com.alcatrazescapee.hexlands.world.HexLandsWorldType;

@Mod(HexLands.MOD_ID)
public final class ForgeHexLands
{
    public ForgeHexLands()
    {
        HexLands.init();

        final IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        HexLandsWorldType.WORLD_TYPES.register(modBus);
        modBus.addListener(this::setup);
    }

    private void setup(FMLCommonSetupEvent event)
    {
        event.enqueueWork(() -> {
            HexLandsWorldType.setDefault();
            HexLands.registerCodecs();
        });
    }
}