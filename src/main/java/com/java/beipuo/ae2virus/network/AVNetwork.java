package com.java.beipuo.ae2virus.network;

import com.java.beipuo.ae2virus.network.packet.OpenVirusTerminalPacket;
import com.java.beipuo.ae2virus.network.packet.SyncVirusInfoPacket;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class AVNetwork {
    private AVNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(SyncVirusInfoPacket.TYPE, SyncVirusInfoPacket.STREAM_CODEC,
                AVNetwork::handleSyncVirusInfo);
        registrar.playToClient(OpenVirusTerminalPacket.TYPE, OpenVirusTerminalPacket.STREAM_CODEC,
                AVNetwork::handleOpenVirusTerminal);
    }

    private static void handleSyncVirusInfo(SyncVirusInfoPacket packet, IPayloadContext context) {
        com.java.beipuo.ae2virus.client.network.AVClientPayloadHandlers.handle(packet, context);
    }

    private static void handleOpenVirusTerminal(OpenVirusTerminalPacket packet, IPayloadContext context) {
        com.java.beipuo.ae2virus.client.network.AVClientPayloadHandlers.handle(packet, context);
    }
}
