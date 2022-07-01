package com.alcatrazescapee.hexlands;

import static com.alcatrazescapee.hexlands.HexLands.MOD_ID;
import net.minecraft.data.worldgen.placement.EndPlacements;
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

import com.alcatrazescapee.hexlands.world.HexLandsWorldType;

@Mod(MOD_ID)
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