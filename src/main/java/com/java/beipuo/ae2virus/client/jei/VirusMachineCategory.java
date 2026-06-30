package com.java.beipuo.ae2virus.client.jei;

import com.java.beipuo.ae2virus.Ae2virus;
import com.java.beipuo.ae2virus.recipe.VirusAssemblyRecipe;
import com.java.beipuo.ae2virus.recipe.VirusBreedingRecipe;
import com.java.beipuo.ae2virus.recipe.VirusCoreManufacturingRecipe;
import com.java.beipuo.ae2virus.registry.AVItems;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;

public class VirusMachineCategory<T extends Recipe<?>> implements IRecipeCategory<RecipeHolder<T>> {
    private final RecipeType<RecipeHolder<T>> recipeType;
    private final Component title;
    private final IDrawable icon;
    private final IDrawable background;
    private boolean currentRecipeUsesDataStream;

    public VirusMachineCategory(RecipeType<RecipeHolder<T>> recipeType, Component title, ItemStack icon, IGuiHelper guiHelper) {
        this.recipeType = recipeType;
        this.title = title;
        this.icon = guiHelper.createDrawableItemStack(icon);
        this.background = guiHelper.createBlankDrawable(168, 75);
    }

    @Override
    public RecipeType<RecipeHolder<T>> getRecipeType() {
        return this.recipeType;
    }

    @Override
    public Component getTitle() {
        return this.title;
    }

    @Override
    public IDrawable getBackground() {
        return this.background;
    }

    @Override
    public int getWidth() {
        return 168;
    }

    @Override
    public int getHeight() {
        return 75;
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<T> holder, IFocusGroup focuses) {
        Object recipe = holder.value();
        if (recipe instanceof VirusCoreManufacturingRecipe coreRecipe) {
            this.currentRecipeUsesDataStream = true;
            addInputs(builder, List.of(coreRecipe.formingCore(), coreRecipe.destructionCore(), coreRecipe.shell()));
            addDataStream(builder);
            builder.addOutputSlot(113, 28).addItemStack(AVItems.VIRUS_CORE.toStack());
        } else if (recipe instanceof VirusAssemblyRecipe assemblyRecipe) {
            this.currentRecipeUsesDataStream = false;
            addInputs(builder, List.of(assemblyRecipe.core(), assemblyRecipe.shell()));
            builder.addOutputSlot(113, 28).addItemStacks(List.of(
                    AVItems.T1_BASIC_VIRUS.toStack(),
                    AVItems.T2_FUSION_VIRUS.toStack(),
                    AVItems.T2_SPECIALIZED_VIRUS.toStack(),
                    AVItems.T2_DEDICATED_VIRUS.toStack(),
                    AVItems.T3_RULE_VIRUS.toStack()));
        } else if (recipe instanceof VirusBreedingRecipe breedingRecipe) {
            this.currentRecipeUsesDataStream = true;
            addInputs(builder, List.of(breedingRecipe.virusOrCore()));
            addDataStream(builder);
            builder.addOutputSlot(113, 28).addItemStacks(ingredientStacks(breedingRecipe.virusOrCore()));
        }
    }

    @Override
    public void draw(RecipeHolder<T> holder, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
        if (!(holder.value() instanceof VirusCoreManufacturingRecipe || holder.value() instanceof VirusBreedingRecipe)) {
            return;
        }
        graphics.drawString(
                net.minecraft.client.Minecraft.getInstance().font,
                Component.translatable("jei.ae2virus.data_stream_1b"),
                3,
                63,
                0xFF555555,
                false);
    }

    private static void addInputs(IRecipeLayoutBuilder builder, List<Ingredient> ingredients) {
        for (int index = 0; index < ingredients.size(); index++) {
            var ingredient = ingredients.get(index);
            builder.addInputSlot(37 + index % 3 * 18, 9 + index / 3 * 18)
                    .addItemStacks(ingredientStacks(ingredient));
        }
    }

    private static void addDataStream(IRecipeLayoutBuilder builder) {
        builder.addInputSlot(4, 28).addItemStack(AVItems.DATA_STREAM_CAPSULE.toStack());
    }

    private static List<ItemStack> ingredientStacks(Ingredient ingredient) {
        return Arrays.stream(ingredient.getItems()).map(ItemStack::copy).toList();
    }

    public static <T extends Recipe<?>> RecipeType<RecipeHolder<T>> recipeType(String id, Class<T> recipeClass) {
        return RecipeType.create(Ae2virus.MODID, id, (Class) RecipeHolder.class);
    }
}
