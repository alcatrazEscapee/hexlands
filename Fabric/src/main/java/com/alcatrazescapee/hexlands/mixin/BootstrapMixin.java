package com.alcatrazescapee.hexlands.mixin;

import net.minecraft.server.Bootstrap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.alcatrazescapee.hexlands.HexLands;

/**
 * This doesn't depend on Fabric API, however it does need to register *one* thing. Vanilla locks registries, so we need to register really early. Fabric unlocks these when present, but we don't want to depend on it.
 */
@Mixin(Bootstrap.class)
public abstract class BootstrapMixin
{
    @Inject(method = "bootStrap", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/Registry;freezeBuiltins()V"))
    private static void registerModBuiltinElements(CallbackInfo ci)
    {
        HexLands.init();
        HexLands.registerCodecs();
    }
}
