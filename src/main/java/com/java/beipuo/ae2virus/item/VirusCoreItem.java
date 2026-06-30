package com.java.beipuo.ae2virus.item;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class VirusCoreItem extends Item {
    public VirusCoreItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        VirusPayload payload = VirusPayload.get(stack, context.registries());
        if (payload == null) {
            tooltip.add(Component.translatable("tooltip.ae2virus.virus_payload.empty"));
            return;
        }
        VirusPayloadItem.appendPayloadTooltip(payload, tooltip);
    }
}
