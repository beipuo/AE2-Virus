package com.java.beipuo.ae2virus.network.packet;

import com.java.beipuo.ae2virus.Ae2virus;
import com.java.beipuo.ae2virus.infection.T1VirusState;
import com.java.beipuo.ae2virus.infection.T2VirusState;
import com.java.beipuo.ae2virus.infection.T3VirusState;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenVirusTerminalPacket(SyncVirusInfoPacket viruses) implements CustomPacketPayload {
    public static final Type<OpenVirusTerminalPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Ae2virus.MODID, "open_virus_terminal"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenVirusTerminalPacket> STREAM_CODEC = StreamCodec.of(
            OpenVirusTerminalPacket::encode,
            OpenVirusTerminalPacket::decode);

    public static OpenVirusTerminalPacket fromStates(List<T1VirusState> t1States, List<T2VirusState> t2States,
            List<T3VirusState> t3States) {
        return new OpenVirusTerminalPacket(SyncVirusInfoPacket.fromStates(t1States, t2States, t3States));
    }

    @Override
    public Type<OpenVirusTerminalPacket> type() {
        return TYPE;
    }

    private static void encode(RegistryFriendlyByteBuf buffer, OpenVirusTerminalPacket packet) {
        SyncVirusInfoPacket.STREAM_CODEC.encode(buffer, packet.viruses());
    }

    private static OpenVirusTerminalPacket decode(RegistryFriendlyByteBuf buffer) {
        return new OpenVirusTerminalPacket(SyncVirusInfoPacket.STREAM_CODEC.decode(buffer));
    }
}
