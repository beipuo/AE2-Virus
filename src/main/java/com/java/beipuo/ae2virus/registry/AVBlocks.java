package com.java.beipuo.ae2virus.registry;

import com.java.beipuo.ae2virus.block.VirusMachineBlock;
import com.java.beipuo.ae2virus.machine.VirusMachineKind;
import net.neoforged.neoforge.registries.DeferredBlock;

public final class AVBlocks {
    public static final AVDeferredRegister.Blocks BLOCKS = AVDeferredRegister.blocks();

    public static final DeferredBlock<VirusMachineBlock> VIRUS_CORE_MANUFACTURER = BLOCKS.machine(
            "virus_core_manufacturer",
            properties -> new VirusMachineBlock(VirusMachineKind.CORE_MANUFACTURER, properties));
    public static final DeferredBlock<VirusMachineBlock> VIRUS_ASSEMBLER = BLOCKS.machine(
            "virus_assembler",
            properties -> new VirusMachineBlock(VirusMachineKind.ASSEMBLER, properties));
    public static final DeferredBlock<VirusMachineBlock> VIRUS_BREEDER = BLOCKS.machine(
            "virus_breeder",
            properties -> new VirusMachineBlock(VirusMachineKind.BREEDER, properties));

    private AVBlocks() {
    }
}
