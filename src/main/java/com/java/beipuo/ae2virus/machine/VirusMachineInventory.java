package com.java.beipuo.ae2virus.machine;

import appeng.util.inv.AppEngInternalInventory;
import com.java.beipuo.ae2virus.item.DataStreamCapsuleItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;

public class VirusMachineInventory extends AppEngInternalInventory {
    public static final int SLOT_INPUT_0 = 0;
    public static final int SLOT_INPUT_1 = 1;
    public static final int SLOT_INPUT_2 = 2;
    public static final int SLOT_DATA_STREAM = 3;
    public static final int SLOT_OUTPUT = 4;
    public static final int SLOTS = 5;

    private final VirusMachineBlockEntity owner;

    public VirusMachineInventory(VirusMachineBlockEntity owner) {
        super(owner, SLOTS, 64);
        this.owner = owner;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        if (slot == SLOT_OUTPUT) {
            return false;
        }
        if (slot == SLOT_DATA_STREAM) {
            if (!(stack.getItem() instanceof DataStreamCapsuleItem)) {
                return false;
            }
            HolderLookup.Provider registries = this.owner.registryAccess();
            return registries != null && DataStreamCapsuleItem.isFilled(stack, registries);
        }
        return slot < this.owner.kind().itemInputSlots();
    }
}
