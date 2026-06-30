package com.java.beipuo.ae2virus.item;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEItemKey;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public record DataStreamPayload(AEKey target, long infectedAmount, int virusLevel) {
    private static final String KEY_TARGET = "target";
    private static final String KEY_INFECTED_AMOUNT = "infectedAmount";
    private static final String KEY_VIRUS_LEVEL = "virusLevel";

    public static ItemStack write(ItemStack stack, AEKey target, long infectedAmount, int virusLevel,
            HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.put(KEY_TARGET, target.toTagGeneric(registries));
        tag.putLong(KEY_INFECTED_AMOUNT, Math.max(0L, infectedAmount));
        tag.putInt(KEY_VIRUS_LEVEL, Math.max(1, virusLevel));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    public static DataStreamPayload read(ItemStack stack, HolderLookup.Provider registries) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = data.copyTag();
        if (!tag.contains(KEY_TARGET, Tag.TAG_COMPOUND)) {
            return null;
        }

        AEKey target = AEKey.fromTagGeneric(registries, tag.getCompound(KEY_TARGET));
        if (!(target instanceof AEItemKey)) {
            return null;
        }
        return new DataStreamPayload(target, tag.getLong(KEY_INFECTED_AMOUNT),
                Math.max(1, tag.getInt(KEY_VIRUS_LEVEL)));
    }

    public static boolean hasPayload(ItemStack stack, HolderLookup.Provider registries) {
        return read(stack, registries) != null;
    }

    public static boolean hasPayloadTag(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = data.copyTag();
        return tag.contains(KEY_TARGET, Tag.TAG_COMPOUND)
                && tag.contains(KEY_INFECTED_AMOUNT, Tag.TAG_LONG)
                && tag.contains(KEY_VIRUS_LEVEL, Tag.TAG_INT);
    }
}
