package com.java.beipuo.ae2virus.item;

import com.java.beipuo.ae2virus.storage.DataStreamKey;
import com.java.beipuo.ae2virus.storage.DataStreamKeyType;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

public class DataStreamCapsuleItem extends Item {
    private static final String TAG_DATA_STREAM = "data_stream";

    public DataStreamCapsuleItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        DataStreamKey key = getDataStream(stack, context.registries());
        if (key == null) {
            tooltip.add(Component.translatable("tooltip.ae2virus.data_stream_capsule.empty"));
            return;
        }

        tooltip.add(Component.translatable("tooltip.ae2virus.data_stream_capsule.contains",
                key.getDisplayName()));
        tooltip.add(Component.translatable("tooltip.ae2virus.data_stream_capsule.amount",
                DataStreamKeyType.AMOUNT_MB));
    }

    public static boolean isFilled(ItemStack stack, HolderLookup.Provider registries) {
        return getDataStream(stack, registries) != null;
    }

    public static boolean hasDataStreamTag(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.contains(TAG_DATA_STREAM, net.minecraft.nbt.Tag.TAG_COMPOUND);
    }

    @Nullable
    public static DataStreamKey getDataStream(ItemStack stack, HolderLookup.Provider registries) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag == null || !tag.contains(TAG_DATA_STREAM, net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            return null;
        }

        var ops = registries.createSerializationContext(NbtOps.INSTANCE);
        return DataStreamKey.CODEC.parse(ops, tag.getCompound(TAG_DATA_STREAM)).result().orElse(null);
    }

    public static void setDataStream(ItemStack stack, HolderLookup.Provider registries, @Nullable DataStreamKey key) {
        if (key == null) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove(TAG_DATA_STREAM));
            return;
        }

        var ops = registries.createSerializationContext(NbtOps.INSTANCE);
        CompoundTag data = (CompoundTag) DataStreamKey.CODEC.encodeStart(ops, key).getOrThrow();
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.put(TAG_DATA_STREAM, data));
    }
}
