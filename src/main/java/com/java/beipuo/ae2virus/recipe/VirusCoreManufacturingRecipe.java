package com.java.beipuo.ae2virus.recipe;

import com.java.beipuo.ae2virus.item.VirusPayload;
import com.java.beipuo.ae2virus.registry.AVItems;
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

public record VirusCoreManufacturingRecipe(
        Ingredient formingCore,
        Ingredient destructionCore,
        Ingredient shell,
        long dataStreamAmount) implements Recipe<VirusMachineInput> {
    public static final long DEFAULT_DATA_STREAM_AMOUNT = DataStreamKeyType.AMOUNT_MB;

    public static final MapCodec<VirusCoreManufacturingRecipe> CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("forming_core").forGetter(VirusCoreManufacturingRecipe::formingCore),
            Ingredient.CODEC_NONEMPTY.fieldOf("destruction_core").forGetter(VirusCoreManufacturingRecipe::destructionCore),
            Ingredient.CODEC_NONEMPTY.fieldOf("shell").forGetter(VirusCoreManufacturingRecipe::shell),
            com.mojang.serialization.Codec.LONG.optionalFieldOf("data_stream_amount", DEFAULT_DATA_STREAM_AMOUNT)
                    .forGetter(VirusCoreManufacturingRecipe::dataStreamAmount))
            .apply(builder, VirusCoreManufacturingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, VirusCoreManufacturingRecipe> STREAM_CODEC = StreamCodec.of(
            VirusCoreManufacturingRecipe::write,
            VirusCoreManufacturingRecipe::read);

    @Override
    public boolean matches(VirusMachineInput input, Level level) {
        return input.size() >= 3
                && this.formingCore.test(input.getItem(0))
                && this.destructionCore.test(input.getItem(1))
                && this.shell.test(input.getItem(2))
                && input.hasDataStream(this.dataStreamAmount);
    }

    @Override
    public ItemStack assemble(VirusMachineInput input, HolderLookup.Provider registries) {
        if (input.dataStream() == null) {
            return ItemStack.EMPTY;
        }
        ItemStack result = AVItems.VIRUS_CORE.toStack();
        VirusPayload.set(result, registries, VirusPayload.fromDataStream(input.dataStream()));
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 3;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return AVItems.VIRUS_CORE.toStack();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return AVRecipes.VIRUS_CORE_MANUFACTURING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return AVRecipes.VIRUS_CORE_MANUFACTURING.get();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, this.formingCore, this.destructionCore, this.shell);
    }

    private static VirusCoreManufacturingRecipe read(RegistryFriendlyByteBuf buffer) {
        return new VirusCoreManufacturingRecipe(
                Ingredient.CONTENTS_STREAM_CODEC.decode(buffer),
                Ingredient.CONTENTS_STREAM_CODEC.decode(buffer),
                Ingredient.CONTENTS_STREAM_CODEC.decode(buffer),
                buffer.readVarLong());
    }

    private static void write(RegistryFriendlyByteBuf buffer, VirusCoreManufacturingRecipe recipe) {
        Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.formingCore);
        Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.destructionCore);
        Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.shell);
        buffer.writeVarLong(recipe.dataStreamAmount);
    }
}
