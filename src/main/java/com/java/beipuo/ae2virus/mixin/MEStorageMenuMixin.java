package com.java.beipuo.ae2virus.mixin;

import appeng.api.networking.IGridNode;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.storage.StorageHelper;
import appeng.helpers.InventoryAction;
import appeng.menu.me.common.MEStorageMenu;
import com.java.beipuo.ae2virus.infection.IVirusNetworkService;
import com.java.beipuo.ae2virus.network.packet.SyncVirusInfoPacket;
import com.java.beipuo.ae2virus.registry.AVItems;
import com.java.beipuo.ae2virus.storage.DataStreamKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MEStorageMenu.class)
public abstract class MEStorageMenuMixin {
    @Unique
    private int ae2virus$lastSyncedInfectionVersion = Integer.MIN_VALUE;
    @Unique
    private int ae2virus$syncCooldown;

    @Shadow
    @Nullable
    public abstract IGridNode getGridNode();

    @Shadow
    protected abstract boolean canInteractWithGrid();

    @Inject(method = "broadcastChanges", at = @At("TAIL"))
    private void ae2virus$syncVirusInfo(CallbackInfo ci) {
        if (!(((MEStorageMenu) (Object) this).getPlayer() instanceof ServerPlayer player)) {
            return;
        }

        IGridNode node = getGridNode();
        if (node == null || !node.isActive()) {
            return;
        }

        IVirusNetworkService service = node.getGrid().getService(IVirusNetworkService.class);
        int version = service.getInfectionVersion();
        if (version != this.ae2virus$lastSyncedInfectionVersion || this.ae2virus$syncCooldown-- <= 0) {
            this.ae2virus$lastSyncedInfectionVersion = version;
            this.ae2virus$syncCooldown = 20;
            PacketDistributor.sendToPlayer(player, SyncVirusInfoPacket.fromStates(service.t1Viruses(),
                    service.t2Viruses(), service.t3Viruses()));
        }
    }

    @Inject(method = "handleNetworkInteraction", at = @At("HEAD"), cancellable = true)
    private void ae2virus$blockInfectedExtraction(ServerPlayer player, @Nullable AEKey clickedKey,
            InventoryAction action, CallbackInfo ci) {
        ae2virus$grabEmptyCapsuleForDataStream(clickedKey, action);

        if (clickedKey == null || clickedKey instanceof DataStreamKey || !ae2virus$isExtractionAction(action)) {
            return;
        }
        if ((action == InventoryAction.PICKUP_OR_SET_DOWN || action == InventoryAction.SPLIT_OR_PLACE_SINGLE)
                && !((MEStorageMenu) (Object) this).getCarried().isEmpty()) {
            return;
        }

        IGridNode node = getGridNode();
        if (node == null || !node.isActive()) {
            return;
        }

        IVirusNetworkService service = node.getGrid().getService(IVirusNetworkService.class);
        if (service.allowedExtraction(clickedKey, 1L) <= 0L) {
            ci.cancel();
        }
    }

    @Unique
    private void ae2virus$grabEmptyCapsuleForDataStream(@Nullable AEKey clickedKey, InventoryAction action) {
        if (!(clickedKey instanceof DataStreamKey) || !ae2virus$isFillAction(action)
                || !((MEStorageMenu) (Object) this).getCarried().isEmpty() || !canInteractWithGrid()) {
            return;
        }

        IGridNode node = getGridNode();
        if (node == null || !node.isActive()) {
            return;
        }

        var menu = (MEStorageMenu) (Object) this;
        var storage = node.getGrid().getStorageService().getInventory();
        var energy = node.getGrid().getEnergyService();
        long extracted = StorageHelper.poweredExtraction(
                energy,
                storage,
                AEItemKey.of(AVItems.DATA_STREAM_CAPSULE.get()),
                1,
                menu.getActionSource());
        if (extracted > 0L) {
            menu.setCarried(new ItemStack(AVItems.DATA_STREAM_CAPSULE.get(), 1));
        }
    }

    private static boolean ae2virus$isExtractionAction(InventoryAction action) {
        return switch (action) {
            case FILL_ITEM, FILL_ITEM_MOVE_TO_PLAYER, FILL_ENTIRE_ITEM, FILL_ENTIRE_ITEM_MOVE_TO_PLAYER,
                    SHIFT_CLICK, MOVE_REGION, PICKUP_SINGLE, ROLL_UP, PICKUP_OR_SET_DOWN,
                    SPLIT_OR_PLACE_SINGLE, CREATIVE_DUPLICATE -> true;
            default -> false;
        };
    }

    private static boolean ae2virus$isFillAction(InventoryAction action) {
        return switch (action) {
            case FILL_ITEM, FILL_ITEM_MOVE_TO_PLAYER, FILL_ENTIRE_ITEM, FILL_ENTIRE_ITEM_MOVE_TO_PLAYER -> true;
            default -> false;
        };
    }
}
