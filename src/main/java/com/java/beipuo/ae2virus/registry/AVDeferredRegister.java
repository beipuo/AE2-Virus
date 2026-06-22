package com.java.beipuo.ae2virus.registry;

import com.java.beipuo.ae2virus.Ae2virus;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class AVDeferredRegister<T> extends DeferredRegister<T> {
    protected AVDeferredRegister(ResourceLocation registryName) {
        super(ResourceKeyHelper.registry(registryName), Ae2virus.MODID);
    }

    public static Blocks blocks() {
        return new Blocks();
    }

    public static Items items() {
        return new Items();
    }

    public static <T> AVDeferredRegister<T> registry(ResourceLocation registryName) {
        return new AVDeferredRegister<>(registryName);
    }

    public static final class Blocks extends DeferredRegister.Blocks {
        private Blocks() {
            super(Ae2virus.MODID);
        }

        public DeferredBlock<Block> machine(String name) {
            return registerSimpleBlock(name, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0F, 6.0F)
                    .requiresCorrectToolForDrops());
        }

        public <B extends Block> DeferredBlock<B> machine(String name, Function<BlockBehaviour.Properties, ? extends B> factory) {
            return registerBlock(name, factory, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0F, 6.0F)
                    .requiresCorrectToolForDrops());
        }
    }

    public static final class Items extends DeferredRegister.Items {
        private Items() {
            super(Ae2virus.MODID);
        }

        public DeferredItem<BlockItem> blockItem(DeferredBlock<? extends Block> block) {
            return registerSimpleBlockItem(block);
        }

        public DeferredItem<Item> material(String name) {
            return registerSimpleItem(name, new Item.Properties());
        }

        public DeferredItem<Item> singleStack(String name) {
            return registerSimpleItem(name, new Item.Properties().stacksTo(1));
        }

        public <I extends Item> DeferredItem<I> singleStack(String name, Function<Item.Properties, ? extends I> factory) {
            return registerItem(name, factory, new Item.Properties().stacksTo(1));
        }
    }

    private static final class ResourceKeyHelper {
        private ResourceKeyHelper() {
        }

        private static <T> net.minecraft.resources.ResourceKey<? extends Registry<T>> registry(ResourceLocation registryName) {
            return net.minecraft.resources.ResourceKey.createRegistryKey(registryName);
        }
    }
}
