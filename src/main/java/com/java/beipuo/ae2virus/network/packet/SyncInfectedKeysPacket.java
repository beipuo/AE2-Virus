package com.java.beipuo.ae2virus.network.packet;

import appeng.api.stacks.AEKey;
import com.java.beipuo.ae2virus.Ae2virus;
import com.java.beipuo.ae2virus.client.infection.ClientVirusState;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncInfectedKeysPacket(Set<AEKey> infectedKeys) implements CustomPacketPayload {
    public static final Type<SyncInfectedKeysPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Ae2virus.MODID, "sync_infected_keys"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncInfectedKeysPacket> STREAM_CODEC = StreamCodec.of(
            SyncInfectedKeysPacket::encode,
            SyncInfectedKeysPacket::decode);

    @Override
    public Type<SyncInfectedKeysPacket> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> ClientVirusState.replaceInfectedKeys(this.infectedKeys));
    }

    private static void encode(RegistryFriendlyByteBuf buffer, SyncInfectedKeysPacket packet) {
        buffer.writeVarInt(packet.infectedKeys.size());
        for (AEKey key : packet.infectedKeys) {
            AEKey.STREAM_CODEC.encode(buffer, key);
        }
    }

    private static SyncInfectedKeysPacket decode(RegistryFriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        Set<AEKey> keys = new HashSet<>(size);
        for (int i = 0; i < size; i++) {
            AEKey key = AEKey.STREAM_CODEC.decode(buffer);
            if (key != null) {
                keys.add(key);
            }
        }
        return new SyncInfectedKeysPacket(keys);
    }
}
