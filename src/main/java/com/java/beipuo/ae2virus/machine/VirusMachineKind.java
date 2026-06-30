package com.java.beipuo.ae2virus.machine;

import com.java.beipuo.ae2virus.registry.AVRecipes;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeType;

public enum VirusMachineKind {
    CORE_MANUFACTURER("virus_core_manufacturer", 3, 200, AVRecipes.VIRUS_CORE_MANUFACTURING),
    ASSEMBLER("virus_assembler", 2, 120, AVRecipes.VIRUS_ASSEMBLY),
    BREEDER("virus_breeder", 1, 80, AVRecipes.VIRUS_BREEDING);

    private final String id;
    private final int itemInputSlots;
    private final int energyCost;
    private final Supplier<? extends RecipeType<?>> recipeType;

    VirusMachineKind(String id, int itemInputSlots, int energyCost, Supplier<? extends RecipeType<?>> recipeType) {
        this.id = id;
        this.itemInputSlots = itemInputSlots;
        this.energyCost = energyCost;
        this.recipeType = recipeType;
    }

    public String id() {
        return this.id;
    }

    public int itemInputSlots() {
        return this.itemInputSlots;
    }

    public int energyCost() {
        return this.energyCost;
    }

    public RecipeType<?> recipeType() {
        return this.recipeType.get();
    }

    public Component title() {
        return Component.translatable("container.ae2virus." + this.id);
    }
}
