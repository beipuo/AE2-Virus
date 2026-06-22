package com.java.beipuo.ae2virus.registry;

import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;

public final class AVBlocks {
    public static final AVDeferredRegister.Blocks BLOCKS = AVDeferredRegister.blocks();

    public static final DeferredBlock<Block> FIREWALL = BLOCKS.machine("firewall");
    public static final DeferredBlock<Block> VIRUS_CORE_MANUFACTURER = BLOCKS.machine("virus_core_manufacturer");
    public static final DeferredBlock<Block> VIRUS_ASSEMBLER = BLOCKS.machine("virus_assembler");
    public static final DeferredBlock<Block> VIRUS_BREEDER = BLOCKS.machine("virus_breeder");
    public static final DeferredBlock<Block> NETWORK_ISOLATION_GATE = BLOCKS.machine("network_isolation_gate");

    private AVBlocks() {
    }
}
