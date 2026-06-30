package com.java.beipuo.ae2virus.item;

import appeng.api.stacks.AEItemKey;
import com.java.beipuo.ae2virus.storage.DataStreamKey;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.Nullable;

public record VirusPayload(AEItemKey target, int virusTier, String t2KindName, int virusLevel, long experience) {
    public static final String TAG_VIRUS = "virus";
    public static final long INITIAL_EXPERIENCE = 1L;

    public static final Codec<VirusPayload> CODEC = RecordCodecBuilder.create(builder -> builder.group(
            AEItemKey.CODEC.fieldOf("target").forGetter(VirusPayload::target),
            Codec.INT.optionalFieldOf("virus_tier", 1).forGetter(VirusPayload::virusTier),
            Codec.STRING.optionalFieldOf("t2_kind", "").forGetter(VirusPayload::t2KindName),
            Codec.INT.fieldOf("virus_level").forGetter(VirusPayload::virusLevel),
            Codec.LONG.optionalFieldOf("experience", INITIAL_EXPERIENCE).forGetter(VirusPayload::experience))
            .apply(builder, VirusPayload::new));

    public VirusPayload {
        virusTier = Math.max(1, Math.min(3, virusTier));
        t2KindName = virusTier == 2 ? t2KindName : "";
        virusLevel = Math.max(1, virusLevel);
        experience = Math.max(0L, experience);
    }

    public static VirusPayload fromDataStream(DataStreamKey dataStream) {
        return new VirusPayload(
                dataStream.target(),
                dataStream.virusTier(),
                dataStream.t2KindName(),
                dataStream.virusLevel(),
                INITIAL_EXPERIENCE);
    }

    public VirusPayload addExperience(long amount) {
        return new VirusPayload(this.target, this.virusTier, this.t2KindName, this.virusLevel,
                this.experience + Math.max(0L, amount));
    }

    public boolean accepts(DataStreamKey dataStream) {
        return this.target.equals(dataStream.target())
                && this.virusTier == dataStream.virusTier()
                && this.t2KindName.equals(dataStream.t2KindName())
                && this.virusLevel == dataStream.virusLevel();
    }

    public static boolean has(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().contains(TAG_VIRUS);
    }

    @Nullable
    public static VirusPayload get(ItemStack stack, HolderLookup.Provider registries) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains(TAG_VIRUS)) {
            return null;
        }
        var ops = registries.createSerializationContext(NbtOps.INSTANCE);
        return CODEC.parse(ops, tag.getCompound(TAG_VIRUS)).result().orElse(null);
    }

    public static void set(ItemStack stack, HolderLookup.Provider registries, VirusPayload payload) {
        var ops = registries.createSerializationContext(NbtOps.INSTANCE);
        CompoundTag data = (CompoundTag) CODEC.encodeStart(ops, payload).getOrThrow();
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.put(TAG_VIRUS, data));
    }
}
