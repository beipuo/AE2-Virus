package com.java.beipuo.ae2virus.item;

import appeng.api.stacks.AmountFormat;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class DataStreamCapsuleItem extends Item {
    public DataStreamCapsuleItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip,
            TooltipFlag flag) {
        DataStreamPayload payload = DataStreamPayload.read(stack, context.registries());
        if (payload == null) {
            tooltip.add(Component.translatable("tooltip.ae2virus.data_stream_capsule.empty")
                    .withStyle(ChatFormatting.DARK_GRAY));
            return;
        }

        tooltip.add(Component.translatable("tooltip.ae2virus.data_stream_capsule.target",
                payload.target().getDisplayName()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.ae2virus.data_stream_capsule.amount",
                payload.target().formatAmount(payload.infectedAmount(), AmountFormat.FULL))
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable("tooltip.ae2virus.data_stream_capsule.level", payload.virusLevel())
                .withStyle(ChatFormatting.AQUA));
    }
}
