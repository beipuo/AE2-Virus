package com.java.beipuo.ae2virus.item;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class VirusPayloadItem extends Item {
    public VirusPayloadItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        VirusPayload payload = VirusPayload.get(stack, context.registries());
        if (payload == null) {
            tooltip.add(Component.translatable("tooltip.ae2virus.virus_payload.empty"));
            return;
        }
        appendPayloadTooltip(payload, tooltip);
    }

    static void appendPayloadTooltip(VirusPayload payload, List<Component> tooltip) {
        tooltip.add(Component.translatable("tooltip.ae2virus.virus_payload.type",
                Component.translatable(virusTypeTranslationKey(payload))));
        tooltip.add(Component.translatable("tooltip.ae2virus.virus_payload.target",
                payload.target().getDisplayName()));
        tooltip.add(Component.translatable("tooltip.ae2virus.virus_payload.level", payload.virusLevel()));
        tooltip.add(Component.translatable("tooltip.ae2virus.virus_payload.experience", payload.experience()));
    }

    private static String virusTypeTranslationKey(VirusPayload payload) {
        if (payload.virusTier() == 3) {
            return "tooltip.ae2virus.virus_terminal.type_t3_rule";
        }
        if (payload.virusTier() == 2) {
            return switch (payload.t2KindName()) {
                case "fusion" -> "tooltip.ae2virus.virus_terminal.type_t2_fusion";
                case "specialized" -> "tooltip.ae2virus.virus_terminal.type_t2_specialized";
                default -> "tooltip.ae2virus.virus_terminal.type_t2_special";
            };
        }
        return "tooltip.ae2virus.virus_terminal.type_t1";
    }
}
