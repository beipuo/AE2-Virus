package com.java.beipuo.ae2virus.network.packet;

import appeng.api.stacks.AEKey;
import com.java.beipuo.ae2virus.Ae2virus;
import com.java.beipuo.ae2virus.infection.T1VirusState;
import com.java.beipuo.ae2virus.infection.T2VirusKind;
import com.java.beipuo.ae2virus.infection.T2VirusState;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SyncVirusInfoPacket(List<T1VirusInfo> t1Viruses, List<T2VirusInfo> t2Viruses)
        implements CustomPacketPayload {
    public static final Type<SyncVirusInfoPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Ae2virus.MODID, "sync_virus_info"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncVirusInfoPacket> STREAM_CODEC = StreamCodec.of(
            SyncVirusInfoPacket::encode,
            SyncVirusInfoPacket::decode);

    public static SyncVirusInfoPacket fromStates(List<T1VirusState> t1States, List<T2VirusState> t2States) {
        Map<AEKey, T1VirusInfo> mergedViruses = new LinkedHashMap<>();
        for (T1VirusState state : t1States) {
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

        List<T2VirusInfo> t2Viruses = new ArrayList<>(t2States.size());
        for (T2VirusState state : t2States) {
            List<T2TargetInfo> targets = new ArrayList<>();
            for (Map.Entry<AEKey, Long> entry : state.blockedAmounts().entrySet()) {
                targets.add(new T2TargetInfo(entry.getKey(), entry.getValue()));
            }
            t2Viruses.add(new T2VirusInfo(state.kind(), state.targetId(), state.experience(), state.level(),
                    state.totalBlockedAmount(), List.copyOf(targets)));
        }
        return new SyncVirusInfoPacket(List.copyOf(mergedViruses.values()), List.copyOf(t2Viruses));
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

        buffer.writeVarInt(packet.t2Viruses.size());
        for (T2VirusInfo virus : packet.t2Viruses) {
            buffer.writeUtf(virus.kind().serializedName());
            buffer.writeResourceLocation(virus.targetId());
            buffer.writeVarLong(virus.experience());
            buffer.writeVarInt(virus.level());
            buffer.writeVarLong(virus.totalBlockedAmount());
            buffer.writeVarInt(virus.targets().size());
            for (T2TargetInfo target : virus.targets()) {
                AEKey.STREAM_CODEC.encode(buffer, target.target());
                buffer.writeVarLong(target.blockedAmount());
            }
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

        int t2Size = buffer.readVarInt();
        List<T2VirusInfo> t2Viruses = new ArrayList<>(t2Size);
        for (int i = 0; i < t2Size; i++) {
            T2VirusKind kind = T2VirusKind.byName(buffer.readUtf());
            ResourceLocation targetId = buffer.readResourceLocation();
            long experience = buffer.readVarLong();
            int level = buffer.readVarInt();
            long totalBlockedAmount = buffer.readVarLong();
            int targetCount = buffer.readVarInt();
            List<T2TargetInfo> targets = new ArrayList<>(targetCount);
            for (int targetIndex = 0; targetIndex < targetCount; targetIndex++) {
                AEKey target = AEKey.STREAM_CODEC.decode(buffer);
                long blockedAmount = buffer.readVarLong();
                if (target != null) {
                    targets.add(new T2TargetInfo(target, blockedAmount));
                }
            }
            t2Viruses.add(new T2VirusInfo(kind, targetId, experience, level, totalBlockedAmount,
                    List.copyOf(targets)));
        }
        return new SyncVirusInfoPacket(List.copyOf(viruses), List.copyOf(t2Viruses));
    }

    public record T1VirusInfo(AEKey target, long blockedAmount, long experience, int level) {
    }

    public record T2VirusInfo(
            T2VirusKind kind,
            ResourceLocation targetId,
            long experience,
            int level,
            long totalBlockedAmount,
            List<T2TargetInfo> targets) {
    }

    public record T2TargetInfo(AEKey target, long blockedAmount) {
    }
}
