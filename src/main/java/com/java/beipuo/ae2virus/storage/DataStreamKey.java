package com.java.beipuo.ae2virus.storage;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import com.java.beipuo.ae2virus.Ae2virus;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class DataStreamKey extends AEKey {
    public static final MapCodec<DataStreamKey> MAP_CODEC = RecordCodecBuilder.mapCodec(
            builder -> builder.group(
                    AEItemKey.CODEC.fieldOf("target").forGetter(DataStreamKey::target),
                    Codec.INT.fieldOf("virus_level").forGetter(DataStreamKey::virusLevel))
                    .apply(builder, DataStreamKey::of));
    public static final Codec<DataStreamKey> CODEC = MAP_CODEC.codec();

    private final AEItemKey target;
    private final int virusLevel;
    private final PrimaryKey primaryKey;
    private final ResourceLocation id;
    private final int hashCode;

    private DataStreamKey(AEItemKey target, int virusLevel) {
        this.target = target;
        this.virusLevel = Math.max(1, virusLevel);
        this.primaryKey = new PrimaryKey(target.getPrimaryKey(), this.virusLevel);
        ResourceLocation targetId = target.getId();
        this.id = ResourceLocation.fromNamespaceAndPath(Ae2virus.MODID,
                "data_stream/" + targetId.getNamespace() + "/" + targetId.getPath() + "/t" + this.virusLevel);
        this.hashCode = 31 * target.hashCode() + this.virusLevel;
    }

    @Nullable
    public static DataStreamKey of(AEKey target, int virusLevel) {
        if (target instanceof AEItemKey itemKey) {
            return of(itemKey, virusLevel);
        }
        return null;
    }

    public static DataStreamKey of(AEItemKey target, int virusLevel) {
        return new DataStreamKey(target, virusLevel);
    }

    public AEItemKey target() {
        return this.target;
    }

    public int virusLevel() {
        return this.virusLevel;
    }

    @Override
    public AEKeyType getType() {
        return DataStreamKeyType.TYPE;
    }

    @Override
    public AEKey dropSecondary() {
        AEItemKey primaryTarget = this.target.dropSecondary();
        return primaryTarget == this.target ? this : of(primaryTarget, this.virusLevel);
    }

    @Override
    public CompoundTag toTag(HolderLookup.Provider registries) {
        var ops = registries.createSerializationContext(NbtOps.INSTANCE);
        return (CompoundTag) CODEC.encodeStart(ops, this).getOrThrow();
    }

    @Override
    public Object getPrimaryKey() {
        return this.primaryKey;
    }

    @Override
    public ResourceLocation getId() {
        return this.id;
    }

    @Override
    public String getModId() {
        return Ae2virus.MODID;
    }

    @Override
    public void writeToPacket(RegistryFriendlyByteBuf data) {
        this.target.writeToPacket(data);
        data.writeVarInt(this.virusLevel);
    }

    public static DataStreamKey fromPacket(RegistryFriendlyByteBuf data) {
        return of(AEItemKey.fromPacket(data), data.readVarInt());
    }

    @Override
    protected Component computeDisplayName() {
        return Component.translatable("ae2virus.key.data_stream", this.target.getDisplayName(), this.virusLevel);
    }

    @Override
    public void addDrops(long amount, List<ItemStack> drops, Level level, BlockPos pos) {
    }

    @Override
    public boolean hasComponents() {
        return this.target.hasComponents();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DataStreamKey other)) {
            return false;
        }
        return this.virusLevel == other.virusLevel && this.target.equals(other.target);
    }

    @Override
    public int hashCode() {
        return this.hashCode;
    }

    @Override
    public String toString() {
        return "DataStreamKey{target=" + this.target + ", virusLevel=" + this.virusLevel + '}';
    }

    private record PrimaryKey(Object targetPrimaryKey, int virusLevel) {
    }
}
