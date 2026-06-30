package com.java.beipuo.ae2virus.machine;

import com.java.beipuo.ae2virus.registry.AVMenus;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public class VirusMachineMenu extends AbstractContainerMenu {
    private final VirusMachineBlockEntity machine;
    private final ContainerLevelAccess access;

    public VirusMachineMenu(int id, Inventory playerInventory, VirusMachineBlockEntity machine) {
        super(AVMenus.VIRUS_MACHINE.get(), id);
        this.machine = machine;
        this.access = ContainerLevelAccess.create(machine.getLevel(), machine.getBlockPos());

        addMachineSlots(machine);
        addPlayerInventory(playerInventory);
        addDataSlots(machine.containerData());
    }

    private void addMachineSlots(VirusMachineBlockEntity machine) {
        var inventory = machine.inventory();
        var itemHandler = inventory.toItemHandler();
        addSlot(new SlotItemHandler(itemHandler, VirusMachineInventory.SLOT_INPUT_0, 44, 24));
        addSlot(new SlotItemHandler(itemHandler, VirusMachineInventory.SLOT_INPUT_1, 62, 24));
        addSlot(new SlotItemHandler(itemHandler, VirusMachineInventory.SLOT_INPUT_2, 80, 24));
        addSlot(new SlotItemHandler(itemHandler, VirusMachineInventory.SLOT_DATA_STREAM, 17, 43));
        addSlot(new SlotItemHandler(itemHandler, VirusMachineInventory.SLOT_OUTPUT, 134, 43) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    public VirusMachineKind kind() {
        return this.machine.kind();
    }

    public int progress() {
        return this.machine.progress();
    }

    public int maxProgress() {
        return this.machine.maxProgress();
    }

    public int energy() {
        return this.machine.energy();
    }

    public int maxEnergy() {
        return this.machine.maxEnergy();
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, this.machine.getBlockState().getBlock());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return moved;
        }

        ItemStack stack = slot.getItem();
        moved = stack.copy();
        int machineSlots = VirusMachineInventory.SLOTS;
        int inventoryStart = machineSlots;
        int inventoryEnd = inventoryStart + 27;
        int hotbarEnd = inventoryEnd + 9;

        if (index < machineSlots) {
            if (!moveItemStackTo(stack, inventoryStart, hotbarEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, VirusMachineInventory.SLOT_DATA_STREAM,
                VirusMachineInventory.SLOT_DATA_STREAM + 1, false)
                && !moveItemStackTo(stack, VirusMachineInventory.SLOT_INPUT_0,
                        this.machine.kind().itemInputSlots(), false)) {
            if (index < inventoryEnd) {
                if (!moveItemStackTo(stack, inventoryEnd, hotbarEnd, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, inventoryStart, inventoryEnd, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return moved;
    }

    private static Container emptyContainer() {
        return new SimpleContainer(0);
    }
}
