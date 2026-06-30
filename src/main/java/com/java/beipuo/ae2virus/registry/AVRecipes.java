package com.java.beipuo.ae2virus.registry;

import com.java.beipuo.ae2virus.Ae2virus;
import com.java.beipuo.ae2virus.recipe.VirusAssemblyRecipe;
import com.java.beipuo.ae2virus.recipe.VirusBreedingRecipe;
import com.java.beipuo.ae2virus.recipe.VirusCoreManufacturingRecipe;
import com.java.beipuo.ae2virus.recipe.VirusRecipeSerializer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class AVRecipes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Registries.RECIPE_TYPE,
            Ae2virus.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(
            Registries.RECIPE_SERIALIZER, Ae2virus.MODID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<VirusCoreManufacturingRecipe>> VIRUS_CORE_MANUFACTURING =
            registerType("virus_core_manufacturing");
    public static final DeferredHolder<RecipeType<?>, RecipeType<VirusAssemblyRecipe>> VIRUS_ASSEMBLY =
            registerType("virus_assembly");
    public static final DeferredHolder<RecipeType<?>, RecipeType<VirusBreedingRecipe>> VIRUS_BREEDING =
            registerType("virus_breeding");

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<VirusCoreManufacturingRecipe>> VIRUS_CORE_MANUFACTURING_SERIALIZER =
            RECIPE_SERIALIZERS.register("virus_core_manufacturing", () -> new VirusRecipeSerializer<>(
                    VirusCoreManufacturingRecipe.CODEC, VirusCoreManufacturingRecipe.STREAM_CODEC));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<VirusAssemblyRecipe>> VIRUS_ASSEMBLY_SERIALIZER =
            RECIPE_SERIALIZERS.register("virus_assembly", () -> new VirusRecipeSerializer<>(
                    VirusAssemblyRecipe.CODEC, VirusAssemblyRecipe.STREAM_CODEC));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<VirusBreedingRecipe>> VIRUS_BREEDING_SERIALIZER =
            RECIPE_SERIALIZERS.register("virus_breeding", () -> new VirusRecipeSerializer<>(
                    VirusBreedingRecipe.CODEC, VirusBreedingRecipe.STREAM_CODEC));

    private AVRecipes() {
    }

    private static <T extends Recipe<?>> DeferredHolder<RecipeType<?>, RecipeType<T>> registerType(String id) {
        RecipeType<T> type = RecipeType.simple(ResourceLocation.fromNamespaceAndPath(Ae2virus.MODID, id));
        return RECIPE_TYPES.register(id, () -> type);
    }
}
