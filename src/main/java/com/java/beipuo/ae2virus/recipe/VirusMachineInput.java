package com.java.beipuo.ae2virus.recipe;

import com.java.beipuo.ae2virus.storage.DataStreamKey;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import org.jetbrains.annotations.Nullable;

public record VirusMachineInput(List<ItemStack> items, @Nullable DataStreamKey dataStream, long dataStreamAmount)
        implements RecipeInput {
    @Override
    public ItemStack getItem(int index) {
        return this.items.get(index);
    }

    @Override
    public int size() {
        return this.items.size();
    }

    public boolean hasDataStream(long amount) {
        return this.dataStream != null && this.dataStreamAmount >= amount;
    }
}
