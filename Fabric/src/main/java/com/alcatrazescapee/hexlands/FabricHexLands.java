package com.alcatrazescapee.hexlands;

import net.fabricmc.api.ModInitializer;

public final class FabricHexLands implements ModInitializer
{
    @Override
    public void onInitialize()
    {
        HexLands.init();
        HexLands.registerCodecs();
    }
}
