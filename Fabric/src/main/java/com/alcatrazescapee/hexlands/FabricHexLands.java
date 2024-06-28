package com.alcatrazescapee.hexlands;

import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

public final class FabricHexLands implements ModInitializer
{
    @Override
    public void onInitialize()
    {
        HexLands.init((id, e) -> Registry.register(BuiltInRegistries.CHUNK_GENERATOR, id, e));
    }
}
