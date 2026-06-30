package com.java.beipuo.ae2virus.storage;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import com.java.beipuo.ae2virus.Ae2virus;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public final class DataStreamKeyType extends AEKeyType {
    public static final int AMOUNT_MB = 1000;

    public static final DataStreamKeyType TYPE = new DataStreamKeyType();

    private DataStreamKeyType() {
        super(ResourceLocation.fromNamespaceAndPath(Ae2virus.MODID, "data_stream"),
                DataStreamKey.class,
                Component.translatable("ae2virus.key_type.data_stream"));
    }

    @Override
    public MapCodec<? extends AEKey> codec() {
        return DataStreamKey.MAP_CODEC;
    }

    @Override
    public int getAmountPerByte() {
        return 8 * AMOUNT_MB;
    }

    @Override
    public int getAmountPerOperation() {
        return AMOUNT_MB * 125 / 1000;
    }

    @Override
    public @Nullable AEKey readFromPacket(RegistryFriendlyByteBuf input) {
        return DataStreamKey.fromPacket(input);
    }

    @Override
    public int getAmountPerUnit() {
        return AMOUNT_MB;
    }

    @Override
    public String getUnitSymbol() {
        return "MB";
    }
}
