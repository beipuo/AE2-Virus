package com.java.beipuo.ae2virus.client.network;

import com.java.beipuo.ae2virus.client.gui.VirusTerminalScreen;
import com.java.beipuo.ae2virus.client.infection.ClientVirusState;
import com.java.beipuo.ae2virus.network.packet.OpenVirusTerminalPacket;
import com.java.beipuo.ae2virus.network.packet.SyncVirusInfoPacket;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@OnlyIn(Dist.CLIENT)
public final class AVClientPayloadHandlers {
    private AVClientPayloadHandlers() {
    }

    public static void handle(SyncVirusInfoPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ClientVirusState.replaceT1Viruses(packet.t1Viruses()));
    }

    public static void handle(OpenVirusTerminalPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> VirusTerminalScreen.open(packet.t1Viruses()));
    }
}
