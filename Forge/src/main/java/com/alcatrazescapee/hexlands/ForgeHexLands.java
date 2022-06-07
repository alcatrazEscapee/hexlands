package com.alcatrazescapee.hexlands;

import net.minecraftforge.fml.common.Mod;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

@Mod(HexLands.MOD_ID)
public final class ForgeHexLands
{
    private static final Logger LOGGER = LogUtils.getLogger();

    public ForgeHexLands()
    {
        LOGGER.info("Hello Forge world!");
    }
}