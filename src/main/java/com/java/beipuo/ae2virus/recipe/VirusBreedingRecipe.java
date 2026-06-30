package com.java.beipuo.ae2virus.recipe;

import com.java.beipuo.ae2virus.item.VirusPayload;
import com.java.beipuo.ae2virus.registry.AVRecipes;
import com.java.beipuo.ae2virus.storage.DataStreamKeyType;
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

public record VirusBreedingRecipe(Ingredient virusOrCore, long dataStreamAmount, long experiencePerOperation)
        implements Recipe<VirusMachineInput> {
    public static final MapCodec<VirusBreedingRecipe> CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("virus_or_core").forGetter(VirusBreedingRecipe::virusOrCore),
            com.mojang.serialization.Codec.LONG.optionalFieldOf("data_stream_amount", (long) DataStreamKeyType.AMOUNT_MB)
                    .forGetter(VirusBreedingRecipe::dataStreamAmount),
            com.mojang.serialization.Codec.LONG.optionalFieldOf("experience", 1L)
                    .forGetter(VirusBreedingRecipe::experiencePerOperation))
            .apply(builder, VirusBreedingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, VirusBreedingRecipe> STREAM_CODEC = StreamCodec.of(
            VirusBreedingRecipe::write,
            VirusBreedingRecipe::read);

    @Override
    public boolean matches(VirusMachineInput input, Level level) {
        VirusPayload payload = input.size() >= 1 ? VirusPayload.get(input.getItem(0), level.registryAccess()) : null;
        return payload != null
                && this.virusOrCore.test(input.getItem(0))
                && input.hasDataStream(this.dataStreamAmount)
                && payload.accepts(input.dataStream());
    }

    @Override
    public ItemStack assemble(VirusMachineInput input, HolderLookup.Provider registries) {
        VirusPayload payload = VirusPayload.get(input.getItem(0), registries);
        if (payload == null) {
            return ItemStack.EMPTY;
        }
        ItemStack result = input.getItem(0).copyWithCount(1);
        VirusPayload.set(result, registries, payload.addExperience(this.experiencePerOperation));
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return AVRecipes.VIRUS_BREEDING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return AVRecipes.VIRUS_BREEDING.get();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, this.virusOrCore);
    }

    private static VirusBreedingRecipe read(RegistryFriendlyByteBuf buffer) {
        return new VirusBreedingRecipe(
                Ingredient.CONTENTS_STREAM_CODEC.decode(buffer),
                buffer.readVarLong(),
                buffer.readVarLong());
    }

    private static void write(RegistryFriendlyByteBuf buffer, VirusBreedingRecipe recipe) {
        Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.virusOrCore);
        buffer.writeVarLong(recipe.dataStreamAmount);
        buffer.writeVarLong(recipe.experiencePerOperation);
    }
}
