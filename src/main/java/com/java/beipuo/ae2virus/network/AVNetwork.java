package com.java.beipuo.ae2virus.network;

import com.java.beipuo.ae2virus.network.packet.SyncInfectedKeysPacket;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class AVNetwork {
    private AVNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(SyncInfectedKeysPacket.TYPE, SyncInfectedKeysPacket.STREAM_CODEC,
                SyncInfectedKeysPacket::handle);
    }
}
