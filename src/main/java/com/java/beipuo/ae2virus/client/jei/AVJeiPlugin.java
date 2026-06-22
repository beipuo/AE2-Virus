package com.java.beipuo.ae2virus.client.jei;

import com.java.beipuo.ae2virus.Ae2virus;
import com.java.beipuo.ae2virus.registry.AVItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRuntimeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

@JeiPlugin
public class AVJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Ae2virus.MODID, "jei_plugin");

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerRuntime(@NotNull IRuntimeRegistration registration) {
        registration.getIngredientManager().removeIngredientsAtRuntime(
                VanillaTypes.ITEM_STACK,
                AVItems.DEBUG_ITEMS.stream().map(item -> new ItemStack(item.value())).toList());
    }
}
