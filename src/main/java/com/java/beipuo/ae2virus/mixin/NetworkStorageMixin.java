package com.java.beipuo.ae2virus.mixin;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.me.storage.NetworkStorage;
import com.java.beipuo.ae2virus.infection.VirusNetworkStorageGuards;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NetworkStorage.class)
public abstract class NetworkStorageMixin {
    @Inject(method = "extract", at = @At("HEAD"), cancellable = true)
    private void ae2virus$blockInfectedNetworkExtraction(AEKey what, long amount, Actionable mode,
            IActionSource source, CallbackInfoReturnable<Long> cir) {
        if (VirusNetworkStorageGuards.blocksExtraction((NetworkStorage) (Object) this, what)) {
            cir.setReturnValue(0L);
        }
    }
}
