package com.java.beipuo.ae2virus.network.packet;

import appeng.api.stacks.AEKey;
import com.java.beipuo.ae2virus.Ae2virus;
import com.java.beipuo.ae2virus.infection.T1VirusState;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SyncVirusInfoPacket(List<T1VirusInfo> t1Viruses) implements CustomPacketPayload {
    public static final Type<SyncVirusInfoPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Ae2virus.MODID, "sync_virus_info"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncVirusInfoPacket> STREAM_CODEC = StreamCodec.of(
            SyncVirusInfoPacket::encode,
            SyncVirusInfoPacket::decode);

    public static SyncVirusInfoPacket fromStates(List<T1VirusState> states) {
        Map<AEKey, T1VirusInfo> mergedViruses = new LinkedHashMap<>();
        for (T1VirusState state : states) {
            T1VirusInfo current = mergedViruses.get(state.target());
            if (current == null) {
                mergedViruses.put(state.target(),
                        new T1VirusInfo(state.target(), state.blockedAmount(), state.experience(), state.level()));
            } else {
                long blockedAmount = Math.max(current.blockedAmount(), state.blockedAmount());
                long experience = Math.max(current.experience(), state.experience());
                int level = Math.max(current.level(), state.level());
                mergedViruses.put(state.target(), new T1VirusInfo(state.target(), blockedAmount, experience, level));
            }
        }
        return new SyncVirusInfoPacket(List.copyOf(mergedViruses.values()));
    }

    @Override
    public Type<SyncVirusInfoPacket> type() {
        return TYPE;
    }

    private static void encode(RegistryFriendlyByteBuf buffer, SyncVirusInfoPacket packet) {
        buffer.writeVarInt(packet.t1Viruses.size());
        for (T1VirusInfo virus : packet.t1Viruses) {
            AEKey.STREAM_CODEC.encode(buffer, virus.target());
            buffer.writeVarLong(virus.blockedAmount());
            buffer.writeVarLong(virus.experience());
            buffer.writeVarInt(virus.level());
        }
    }

    private static SyncVirusInfoPacket decode(RegistryFriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        List<T1VirusInfo> viruses = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            AEKey target = AEKey.STREAM_CODEC.decode(buffer);
            long blockedAmount = buffer.readVarLong();
            long experience = buffer.readVarLong();
            int level = buffer.readVarInt();
            if (target != null) {
                viruses.add(new T1VirusInfo(target, blockedAmount, experience, level));
            }
        }
        return new SyncVirusInfoPacket(List.copyOf(viruses));
    }

    public record T1VirusInfo(AEKey target, long blockedAmount, long experience, int level) {
    }
}
