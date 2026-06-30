package com.java.beipuo.ae2virus.registry;

import java.util.List;
import appeng.items.parts.PartItem;
import com.java.beipuo.ae2virus.item.DataStreamCapsuleItem;
import com.java.beipuo.ae2virus.item.DataStreamStorageCellItem;
import com.java.beipuo.ae2virus.item.VirusCoreItem;
import com.java.beipuo.ae2virus.item.VirusPayloadItem;
import com.java.beipuo.ae2virus.item.VirusStimulatorItem;
import com.java.beipuo.ae2virus.part.VirusTerminalPart;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

public final class AVItems {
    public static final AVDeferredRegister.Items ITEMS = AVDeferredRegister.items();

    public static final DeferredItem<BlockItem> VIRUS_CORE_MANUFACTURER = ITEMS.blockItem(AVBlocks.VIRUS_CORE_MANUFACTURER);
    public static final DeferredItem<BlockItem> VIRUS_ASSEMBLER = ITEMS.blockItem(AVBlocks.VIRUS_ASSEMBLER);
    public static final DeferredItem<BlockItem> VIRUS_BREEDER = ITEMS.blockItem(AVBlocks.VIRUS_BREEDER);
    public static final DeferredItem<PartItem<VirusTerminalPart>> VIRUS_TERMINAL = AVParts.VIRUS_TERMINAL;

    public static final DeferredItem<VirusPayloadItem> T1_BASIC_VIRUS = ITEMS.singleStack("t1_basic_virus", VirusPayloadItem::new);
    public static final DeferredItem<VirusPayloadItem> T2_FUSION_VIRUS = ITEMS.singleStack("t2_fusion_virus", VirusPayloadItem::new);
    public static final DeferredItem<VirusPayloadItem> T2_SPECIALIZED_VIRUS = ITEMS.singleStack("t2_specialized_virus", VirusPayloadItem::new);
    public static final DeferredItem<VirusPayloadItem> T2_DEDICATED_VIRUS = ITEMS.singleStack("t2_dedicated_virus", VirusPayloadItem::new);
    public static final DeferredItem<VirusPayloadItem> T3_RULE_VIRUS = ITEMS.singleStack("t3_rule_virus", VirusPayloadItem::new);
    public static final DeferredItem<VirusPayloadItem> CREATIVE_VIRUS = ITEMS.singleStack("creative_virus", VirusPayloadItem::new);
    public static final DeferredItem<VirusCoreItem> VIRUS_CORE = ITEMS.singleStack("virus_core", VirusCoreItem::new);
    public static final DeferredItem<Item> VIRUS_SHELL = ITEMS.material("virus_shell");

    public static final DeferredItem<DataStreamCapsuleItem> DATA_STREAM_CAPSULE = ITEMS.singleStack(
            "data_stream_capsule",
            DataStreamCapsuleItem::new);
    public static final DeferredItem<DataStreamStorageCellItem> DATA_STREAM_STORAGE_CELL = ITEMS.singleStack(
            "data_stream_storage_cell",
            DataStreamStorageCellItem::new);

    public static final DeferredItem<VirusStimulatorItem> VIRUS_STIMULATOR = ITEMS.singleStack(
            "virus_stimulator",
            VirusStimulatorItem::new);

    public static final List<DeferredItem<? extends Item>> VIRUS_INFO_ITEMS = List.of(
            T1_BASIC_VIRUS,
            T2_FUSION_VIRUS,
            T2_SPECIALIZED_VIRUS,
            T2_DEDICATED_VIRUS,
            T3_RULE_VIRUS,
            CREATIVE_VIRUS);

    private AVItems() {
    }
}
