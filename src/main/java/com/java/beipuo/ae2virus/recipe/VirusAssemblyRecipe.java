package com.java.beipuo.ae2virus.recipe;

import com.java.beipuo.ae2virus.item.VirusPayload;
import com.java.beipuo.ae2virus.registry.AVItems;
import com.java.beipuo.ae2virus.registry.AVRecipes;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public record VirusAssemblyRecipe(Ingredient core, Ingredient shell) implements Recipe<VirusMachineInput> {
    public static final MapCodec<VirusAssemblyRecipe> CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("core").forGetter(VirusAssemblyRecipe::core),
            Ingredient.CODEC_NONEMPTY.fieldOf("shell").forGetter(VirusAssemblyRecipe::shell))
            .apply(builder, VirusAssemblyRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, VirusAssemblyRecipe> STREAM_CODEC = StreamCodec.of(
            VirusAssemblyRecipe::write,
            VirusAssemblyRecipe::read);

    @Override
    public boolean matches(VirusMachineInput input, Level level) {
        return input.size() >= 2 && this.core.test(input.getItem(0)) && this.shell.test(input.getItem(1))
                && VirusPayload.has(input.getItem(0));
    }

    @Override
    public ItemStack assemble(VirusMachineInput input, HolderLookup.Provider registries) {
        VirusPayload payload = VirusPayload.get(input.getItem(0), registries);
        if (payload == null) {
            return ItemStack.EMPTY;
        }
        ItemStack result = virusItemFor(payload).toStack();
        VirusPayload.set(result, registries, payload);
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return AVItems.T1_BASIC_VIRUS.toStack();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return AVRecipes.VIRUS_ASSEMBLY_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return AVRecipes.VIRUS_ASSEMBLY.get();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, this.core, this.shell);
    }

    private static net.neoforged.neoforge.registries.DeferredItem<?> virusItemFor(VirusPayload payload) {
        if (payload.virusTier() == 3) {
            return AVItems.T3_RULE_VIRUS;
        }
        if (payload.virusTier() == 2) {
            return switch (payload.t2KindName()) {
                case "fusion" -> AVItems.T2_FUSION_VIRUS;
                case "specialized" -> AVItems.T2_SPECIALIZED_VIRUS;
                default -> AVItems.T2_DEDICATED_VIRUS;
            };
        }
        return AVItems.T1_BASIC_VIRUS;
    }

    private static VirusAssemblyRecipe read(RegistryFriendlyByteBuf buffer) {
        return new VirusAssemblyRecipe(
                Ingredient.CONTENTS_STREAM_CODEC.decode(buffer),
                Ingredient.CONTENTS_STREAM_CODEC.decode(buffer));
    }

    private static void write(RegistryFriendlyByteBuf buffer, VirusAssemblyRecipe recipe) {
        Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.core);
        Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.shell);
    }
}
