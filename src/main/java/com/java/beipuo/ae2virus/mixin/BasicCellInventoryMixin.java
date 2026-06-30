package com.java.beipuo.ae2virus.mixin;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.me.cells.BasicCellInventory;
import com.java.beipuo.ae2virus.item.DataStreamCapsuleItem;
import com.java.beipuo.ae2virus.item.DataStreamStorageCellItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BasicCellInventory.class)
public abstract class BasicCellInventoryMixin {
    @Shadow
    @Final
    private ItemStack i;

    @Inject(method = "insert", at = @At("HEAD"), cancellable = true)
    private void ae2virus$onlyVirusCellStoresDataStreams(AEKey what, long amount, Actionable mode,
            IActionSource source, CallbackInfoReturnable<Long> cir) {
        if (what instanceof AEItemKey itemKey
                && itemKey.getItem() instanceof DataStreamCapsuleItem
                && !(this.i.getItem() instanceof DataStreamStorageCellItem)) {
            cir.setReturnValue(0L);
        }
    }
}
