package com.java.beipuo.ae2virus.registry;

import com.java.beipuo.ae2virus.Ae2virus;
import com.java.beipuo.ae2virus.machine.VirusMachineBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class AVBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(
            Registries.BLOCK_ENTITY_TYPE,
            Ae2virus.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VirusMachineBlockEntity>> VIRUS_MACHINE =
            BLOCK_ENTITIES.register("virus_machine", () -> BlockEntityType.Builder.of(
                    VirusMachineBlockEntity::new,
                    AVBlocks.VIRUS_CORE_MANUFACTURER.get(),
                    AVBlocks.VIRUS_ASSEMBLER.get(),
                    AVBlocks.VIRUS_BREEDER.get())
                    .build(null));

    private AVBlockEntities() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
        modEventBus.addListener(AVBlockEntities::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                VIRUS_MACHINE.get(),
                (machine, side) -> machine.inventory().toItemHandler());
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                VIRUS_MACHINE.get(),
                (machine, side) -> machine.getEnergyStorage(side));
    }
}
