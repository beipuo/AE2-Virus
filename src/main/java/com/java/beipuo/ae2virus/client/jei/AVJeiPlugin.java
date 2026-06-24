package com.java.beipuo.ae2virus.client.jei;

import com.java.beipuo.ae2virus.Ae2virus;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

@JeiPlugin
public class AVJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Ae2virus.MODID, "jei_plugin");

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return UID;
    }

}
