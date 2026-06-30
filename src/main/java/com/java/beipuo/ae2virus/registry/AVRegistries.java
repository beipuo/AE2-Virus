package com.java.beipuo.ae2virus.registry;

import net.neoforged.bus.api.IEventBus;

public final class AVRegistries {
    private AVRegistries() {
    }

    public static void register(IEventBus modEventBus) {
        AVBlocks.BLOCKS.register(modEventBus);
        AVBlockEntities.register(modEventBus);
        AVItems.ITEMS.register(modEventBus);
        AVMenus.MENUS.register(modEventBus);
        AVRecipes.RECIPE_TYPES.register(modEventBus);
        AVRecipes.RECIPE_SERIALIZERS.register(modEventBus);
        AVCreativeTabs.CREATIVE_TABS.register(modEventBus);
    }
}
