package com.java.beipuo.ae2virus.storage;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import com.java.beipuo.ae2virus.Ae2virus;
import com.java.beipuo.ae2virus.infection.T2VirusKind;
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
                    Codec.INT.optionalFieldOf("virus_tier", 1).forGetter(DataStreamKey::virusTier),
                    Codec.STRING.optionalFieldOf("t2_kind", "").forGetter(DataStreamKey::t2KindName),
                    Codec.INT.fieldOf("virus_level").forGetter(DataStreamKey::virusLevel))
                    .apply(builder, DataStreamKey::of));
    public static final Codec<DataStreamKey> CODEC = MAP_CODEC.codec();

    private final AEItemKey target;
    private final int virusTier;
    private final String t2KindName;
    private final int virusLevel;
    private final PrimaryKey primaryKey;
    private final ResourceLocation id;
    private final int hashCode;

    private DataStreamKey(AEItemKey target, int virusTier, String t2KindName, int virusLevel) {
        this.target = target;
        this.virusTier = Math.max(1, Math.min(3, virusTier));
        this.t2KindName = this.virusTier == 2 ? t2KindName : "";
        this.virusLevel = Math.max(1, virusLevel);
        this.primaryKey = new PrimaryKey(target.getPrimaryKey(), this.virusTier, this.t2KindName, this.virusLevel);
        ResourceLocation targetId = target.getId();
        this.id = ResourceLocation.fromNamespaceAndPath(Ae2virus.MODID,
                "data_stream/" + targetId.getNamespace() + "/" + targetId.getPath() + "/t" + this.virusTier + "/"
                        + this.t2KindName + "/l" + this.virusLevel);
        this.hashCode = (((31 * target.hashCode() + this.virusTier) * 31 + this.t2KindName.hashCode()) * 31
                + this.virusLevel);
    }

    @Nullable
    public static DataStreamKey of(AEKey target, int virusLevel) {
        return of(target, 1, "", virusLevel);
    }

    @Nullable
    public static DataStreamKey of(AEKey target, int virusTier, @Nullable T2VirusKind t2Kind, int virusLevel) {
        return of(target, virusTier, t2Kind == null ? "" : t2Kind.serializedName(), virusLevel);
    }

    @Nullable
    public static DataStreamKey of(AEKey target, int virusTier, String t2KindName, int virusLevel) {
        if (target instanceof AEItemKey itemKey) {
            return of(itemKey, virusTier, t2KindName, virusLevel);
        }
        return null;
    }

    public static DataStreamKey of(AEItemKey target, int virusLevel) {
        return of(target, 1, "", virusLevel);
    }

    public static DataStreamKey of(AEItemKey target, int virusTier, String t2KindName, int virusLevel) {
        return new DataStreamKey(target, virusTier, t2KindName, virusLevel);
    }

    public AEItemKey target() {
        return this.target;
    }

    public int virusTier() {
        return this.virusTier;
    }

    public String t2KindName() {
        return this.t2KindName;
    }

    public T2VirusKind t2Kind() {
        return T2VirusKind.byName(this.t2KindName);
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
        return primaryTarget == this.target ? this
                : of(primaryTarget, this.virusTier, this.t2KindName, this.virusLevel);
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
        data.writeVarInt(this.virusTier);
        data.writeUtf(this.t2KindName);
        data.writeVarInt(this.virusLevel);
    }

    public static DataStreamKey fromPacket(RegistryFriendlyByteBuf data) {
        return of(AEItemKey.fromPacket(data), data.readVarInt(), data.readUtf(), data.readVarInt());
    }

    @Override
    protected Component computeDisplayName() {
        return Component.translatable("ae2virus.key.data_stream",
                Component.translatable(virusTypeTranslationKey()),
                this.target.getDisplayName());
    }

    public String virusTypeTranslationKey() {
        if (this.virusTier == 3) {
            return "tooltip.ae2virus.virus_terminal.type_t3_rule";
        }
        if (this.virusTier == 2) {
            return switch (t2Kind()) {
                case FUSION -> "tooltip.ae2virus.virus_terminal.type_t2_fusion";
                case SPECIALIZED -> "tooltip.ae2virus.virus_terminal.type_t2_specialized";
                case SPECIAL_RESOURCE -> "tooltip.ae2virus.virus_terminal.type_t2_special";
            };
        }
        return "tooltip.ae2virus.virus_terminal.type_t1";
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
        return this.virusTier == other.virusTier
                && this.virusLevel == other.virusLevel
                && this.t2KindName.equals(other.t2KindName)
                && this.target.equals(other.target);
    }

    @Override
    public int hashCode() {
        return this.hashCode;
    }

    @Override
    public String toString() {
        return "DataStreamKey{target=" + this.target + ", virusTier=" + this.virusTier
                + ", t2KindName='" + this.t2KindName + "', virusLevel=" + this.virusLevel + '}';
    }

    private record PrimaryKey(Object targetPrimaryKey, int virusTier, String t2KindName, int virusLevel) {
    }
}
