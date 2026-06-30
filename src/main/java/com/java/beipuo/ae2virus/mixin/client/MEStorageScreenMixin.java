package com.java.beipuo.ae2virus.mixin.client;

import appeng.client.gui.me.common.MEStorageScreen;
import appeng.client.gui.me.common.RepoSlot;
import appeng.helpers.InventoryAction;
import appeng.menu.me.common.GridInventoryEntry;
import com.java.beipuo.ae2virus.client.infection.ClientVirusState;
import com.java.beipuo.ae2virus.item.DataStreamCapsuleItem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MEStorageScreen.class)
public abstract class MEStorageScreenMixin {
    @Inject(method = "renderSlot", at = @At(value = "INVOKE", target = "Lappeng/api/client/AEKeyRendering;drawInGui(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/gui/GuiGraphics;IILappeng/api/stacks/AEKey;)V", shift = At.Shift.AFTER))
    private void ae2virus$renderInfectionOverlay(GuiGraphics guiGraphics, Slot slot, CallbackInfo ci) {
        if (!(slot instanceof RepoSlot repoSlot)) {
            return;
        }

        GridInventoryEntry entry = repoSlot.getEntry();
        if (entry != null && ClientVirusState.blockedAmount(entry.getWhat()) > 0L) {
            guiGraphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, 0x99A0A0A0);
        }
    }

    @Inject(method = "handleGridInventoryEntryMouseClick", at = @At("HEAD"), cancellable = true)
    private void ae2virus$emptyDataStreamCapsule(GridInventoryEntry entry, int mouseButton, ClickType clickType,
            CallbackInfo ci) {
        var menu = ((MEStorageScreen<?>) (Object) this).getMenu();
        if (mouseButton != 1 || menu.getCarried().isEmpty()) {
            return;
        }

        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        var key = DataStreamCapsuleItem.getDataStream(menu.getCarried(), player.level().registryAccess());
        if (key == null || !menu.isKeyVisible(key)) {
            return;
        }

        menu.handleInteraction(-1, clickType == ClickType.QUICK_MOVE
                ? InventoryAction.EMPTY_ENTIRE_ITEM
                : InventoryAction.EMPTY_ITEM);
        ci.cancel();
    }
}
