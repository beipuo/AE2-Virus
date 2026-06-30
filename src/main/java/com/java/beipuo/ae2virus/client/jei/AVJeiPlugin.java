package com.java.beipuo.ae2virus.client.jei;

import com.java.beipuo.ae2virus.Ae2virus;
import com.java.beipuo.ae2virus.recipe.VirusAssemblyRecipe;
import com.java.beipuo.ae2virus.recipe.VirusBreedingRecipe;
import com.java.beipuo.ae2virus.recipe.VirusCoreManufacturingRecipe;
import com.java.beipuo.ae2virus.registry.AVBlocks;
import com.java.beipuo.ae2virus.registry.AVRecipes;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.NotNull;

@JeiPlugin
public class AVJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Ae2virus.MODID, "jei_plugin");
    private static final RecipeType<RecipeHolder<VirusCoreManufacturingRecipe>> VIRUS_CORE_MANUFACTURING =
            VirusMachineCategory.recipeType("virus_core_manufacturing", VirusCoreManufacturingRecipe.class);
    private static final RecipeType<RecipeHolder<VirusAssemblyRecipe>> VIRUS_ASSEMBLY =
            VirusMachineCategory.recipeType("virus_assembly", VirusAssemblyRecipe.class);
    private static final RecipeType<RecipeHolder<VirusBreedingRecipe>> VIRUS_BREEDING =
            VirusMachineCategory.recipeType("virus_breeding", VirusBreedingRecipe.class);

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registry) {
        var helper = registry.getJeiHelpers().getGuiHelper();
        registry.addRecipeCategories(
                new VirusMachineCategory<>(VIRUS_CORE_MANUFACTURING, AVBlocks.VIRUS_CORE_MANUFACTURER.get().getName(),
                        AVBlocks.VIRUS_CORE_MANUFACTURER.toStack(), helper),
                new VirusMachineCategory<>(VIRUS_ASSEMBLY, AVBlocks.VIRUS_ASSEMBLER.get().getName(),
                        AVBlocks.VIRUS_ASSEMBLER.toStack(), helper),
                new VirusMachineCategory<>(VIRUS_BREEDING, AVBlocks.VIRUS_BREEDER.get().getName(),
                        AVBlocks.VIRUS_BREEDER.toStack(), helper));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var level = net.minecraft.client.Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        registration.addRecipes(VIRUS_CORE_MANUFACTURING,
                List.copyOf(level.getRecipeManager().getAllRecipesFor(AVRecipes.VIRUS_CORE_MANUFACTURING.get())));
        registration.addRecipes(VIRUS_ASSEMBLY,
                List.copyOf(level.getRecipeManager().getAllRecipesFor(AVRecipes.VIRUS_ASSEMBLY.get())));
        registration.addRecipes(VIRUS_BREEDING,
                List.copyOf(level.getRecipeManager().getAllRecipesFor(AVRecipes.VIRUS_BREEDING.get())));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(AVBlocks.VIRUS_CORE_MANUFACTURER.asItem(), VIRUS_CORE_MANUFACTURING);
        registration.addRecipeCatalyst(AVBlocks.VIRUS_ASSEMBLER.asItem(), VIRUS_ASSEMBLY);
        registration.addRecipeCatalyst(AVBlocks.VIRUS_BREEDER.asItem(), VIRUS_BREEDING);
    }
}
