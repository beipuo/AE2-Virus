package com.java.beipuo.ae2virus.client;

import appeng.api.client.AEKeyRendering;
import com.java.beipuo.ae2virus.Ae2virus;
import com.java.beipuo.ae2virus.storage.DataStreamKey;
import com.java.beipuo.ae2virus.storage.DataStreamKeyType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@Mod(value = Ae2virus.MODID, dist = Dist.CLIENT)
public final class AVClient {
    public AVClient(IEventBus modEventBus) {
        modEventBus.addListener(this::clientSetup);
    }

    private void clientSetup(FMLClientSetupEvent event) {
        AEKeyRendering.register(DataStreamKeyType.TYPE, DataStreamKey.class, new DataStreamKeyRenderHandler());
    }
}
