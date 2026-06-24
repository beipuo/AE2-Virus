package com.java.beipuo.ae2virus.network.packet;

import appeng.api.stacks.AEKey;
import com.java.beipuo.ae2virus.Ae2virus;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenVirusTerminalPacket(List<SyncVirusInfoPacket.T1VirusInfo> t1Viruses) implements CustomPacketPayload {
    public static final Type<OpenVirusTerminalPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Ae2virus.MODID, "open_virus_terminal"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenVirusTerminalPacket> STREAM_CODEC = StreamCodec.of(
            OpenVirusTerminalPacket::encode,
            OpenVirusTerminalPacket::decode);

    public static OpenVirusTerminalPacket fromStates(List<com.java.beipuo.ae2virus.infection.T1VirusState> states) {
        return new OpenVirusTerminalPacket(SyncVirusInfoPacket.fromStates(states).t1Viruses());
    }

    @Override
    public Type<OpenVirusTerminalPacket> type() {
        return TYPE;
    }

    private static void encode(RegistryFriendlyByteBuf buffer, OpenVirusTerminalPacket packet) {
        buffer.writeVarInt(packet.t1Viruses.size());
        for (SyncVirusInfoPacket.T1VirusInfo virus : packet.t1Viruses) {
            AEKey.STREAM_CODEC.encode(buffer, virus.target());
            buffer.writeVarLong(virus.blockedAmount());
            buffer.writeVarLong(virus.experience());
            buffer.writeVarInt(virus.level());
        }
    }

    private static OpenVirusTerminalPacket decode(RegistryFriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        List<SyncVirusInfoPacket.T1VirusInfo> viruses = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            AEKey target = AEKey.STREAM_CODEC.decode(buffer);
            long blockedAmount = buffer.readVarLong();
            long experience = buffer.readVarLong();
            int level = buffer.readVarInt();
            if (target != null) {
                viruses.add(new SyncVirusInfoPacket.T1VirusInfo(target, blockedAmount, experience, level));
            }
        }
        return new OpenVirusTerminalPacket(List.copyOf(viruses));
    }
}
