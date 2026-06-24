package com.java.beipuo.ae2virus.registry;

import com.java.beipuo.ae2virus.Ae2virus;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class AVCreativeTabs {
    public static final AVDeferredRegister<CreativeModeTab> CREATIVE_TABS = AVDeferredRegister.registry(Registries.CREATIVE_MODE_TAB.location());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = CREATIVE_TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.ae2virus.main"))
            .withTabsBefore(CreativeModeTabs.FUNCTIONAL_BLOCKS)
            .icon(AVBlocks.FIREWALL::toStack)
            .displayItems((parameters, output) -> {
                AVItems.ITEMS.getEntries().stream()
                        .filter(item -> !AVItems.VIRUS_INFO_ITEMS.contains(item))
                        .forEach(item -> output.accept(item.value()));
            })
            .build());

    private AVCreativeTabs() {
    }
}
