package com.java.beipuo.ae2virus.registry;

import appeng.api.parts.IPart;
import appeng.api.parts.IPartItem;
import appeng.api.parts.PartModels;
import appeng.items.parts.PartItem;
import appeng.items.parts.PartModelsHelper;
import com.java.beipuo.ae2virus.part.VirusTerminalPart;
import java.util.function.Function;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

public final class AVParts {
    public static final DeferredItem<PartItem<VirusTerminalPart>> VIRUS_TERMINAL = createPart(
            "virus_terminal",
            VirusTerminalPart.class,
            VirusTerminalPart::new);

    private AVParts() {
    }

    private static <T extends IPart> DeferredItem<PartItem<T>> createPart(
            String name,
            Class<T> partClass,
            Function<IPartItem<T>, T> factory) {
        PartModels.registerModels(PartModelsHelper.createModels(partClass));
        return AVItems.ITEMS.registerItem(name, props -> new PartItem<>(props, partClass, factory),
                new Item.Properties().stacksTo(1));
    }
}
