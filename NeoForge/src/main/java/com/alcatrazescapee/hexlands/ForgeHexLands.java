package com.alcatrazescapee.hexlands;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(HexLands.MOD_ID)
public final class ForgeHexLands
{
    private final DeferredRegister<MapCodec<? extends ChunkGenerator>> registry = DeferredRegister.create(BuiltInRegistries.CHUNK_GENERATOR, HexLands.MOD_ID);

    public ForgeHexLands(IEventBus bus)
    {
        HexLands.init((id, e) -> registry.register(id.getPath(), () -> e));
        registry.register(bus);
    }
}