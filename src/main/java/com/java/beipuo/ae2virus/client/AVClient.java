package com.java.beipuo.ae2virus.client;

import appeng.api.client.AEKeyRendering;
import com.java.beipuo.ae2virus.Ae2virus;
import com.java.beipuo.ae2virus.client.gui.VirusMachineScreen;
import com.java.beipuo.ae2virus.item.DataStreamCapsuleItem;
import com.java.beipuo.ae2virus.registry.AVItems;
import com.java.beipuo.ae2virus.registry.AVMenus;
import com.java.beipuo.ae2virus.storage.DataStreamKey;
import com.java.beipuo.ae2virus.storage.DataStreamKeyType;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@Mod(value = Ae2virus.MODID, dist = Dist.CLIENT)
public final class AVClient {
    private static final ResourceLocation FILLED_PREDICATE = ResourceLocation.fromNamespaceAndPath(
            Ae2virus.MODID,
            "filled");

    public AVClient(IEventBus modEventBus) {
        modEventBus.addListener(this::clientSetup);
        modEventBus.addListener(this::registerScreens);
    }

    private void clientSetup(FMLClientSetupEvent event) {
        AEKeyRendering.register(DataStreamKeyType.TYPE, DataStreamKey.class, new DataStreamKeyRenderHandler());
        event.enqueueWork(() -> ItemProperties.register(
                AVItems.DATA_STREAM_CAPSULE.get(),
                FILLED_PREDICATE,
                (stack, level, entity, seed) -> DataStreamCapsuleItem.hasDataStreamTag(stack) ? 1.0F : 0.0F));
    }

    private void registerScreens(RegisterMenuScreensEvent event) {
        event.register(AVMenus.VIRUS_MACHINE.get(), VirusMachineScreen::new);
    }
}
