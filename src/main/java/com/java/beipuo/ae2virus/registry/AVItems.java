package com.java.beipuo.ae2virus.registry;

import java.util.List;
import appeng.items.parts.PartItem;
import com.java.beipuo.ae2virus.part.VirusTerminalPart;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

public final class AVItems {
    public static final AVDeferredRegister.Items ITEMS = AVDeferredRegister.items();

    public static final DeferredItem<BlockItem> FIREWALL = ITEMS.blockItem(AVBlocks.FIREWALL);
    public static final DeferredItem<BlockItem> VIRUS_CORE_MANUFACTURER = ITEMS.blockItem(AVBlocks.VIRUS_CORE_MANUFACTURER);
    public static final DeferredItem<BlockItem> VIRUS_ASSEMBLER = ITEMS.blockItem(AVBlocks.VIRUS_ASSEMBLER);
    public static final DeferredItem<BlockItem> VIRUS_BREEDER = ITEMS.blockItem(AVBlocks.VIRUS_BREEDER);
    public static final DeferredItem<BlockItem> NETWORK_ISOLATION_GATE = ITEMS.blockItem(AVBlocks.NETWORK_ISOLATION_GATE);
    public static final DeferredItem<PartItem<VirusTerminalPart>> VIRUS_TERMINAL = AVParts.VIRUS_TERMINAL;

    public static final DeferredItem<Item> TARGETED_VIRUS_SHELL = ITEMS.material("targeted_virus_shell");
    public static final DeferredItem<Item> BROAD_SPECTRUM_VIRUS_SHELL = ITEMS.material("broad_spectrum_virus_shell");
    public static final DeferredItem<Item> SYSTEMIC_VIRUS_SHELL = ITEMS.material("systemic_virus_shell");
    public static final DeferredItem<Item> POLYMORPHIC_VIRUS_SHELL = ITEMS.material("polymorphic_virus_shell");

    public static final DeferredItem<Item> TARGETED_VIRUS_CORE = ITEMS.material("targeted_virus_core");
    public static final DeferredItem<Item> BROAD_SPECTRUM_VIRUS_CORE = ITEMS.material("broad_spectrum_virus_core");
    public static final DeferredItem<Item> SYSTEMIC_VIRUS_CORE = ITEMS.material("systemic_virus_core");
    public static final DeferredItem<Item> POLYMORPHIC_VIRUS_CORE = ITEMS.material("polymorphic_virus_core");

    public static final DeferredItem<Item> TARGETED_VIRUS = ITEMS.singleStack("targeted_virus");
    public static final DeferredItem<Item> BROAD_SPECTRUM_VIRUS = ITEMS.singleStack("broad_spectrum_virus");
    public static final DeferredItem<Item> SYSTEMIC_VIRUS = ITEMS.singleStack("systemic_virus");
    public static final DeferredItem<Item> POLYMORPHIC_VIRUS = ITEMS.singleStack("polymorphic_virus");

    public static final DeferredItem<Item> T1_BASIC_VIRUS = ITEMS.singleStack("t1_basic_virus");
    public static final DeferredItem<Item> T2_FUSION_VIRUS = ITEMS.singleStack("t2_fusion_virus");
    public static final DeferredItem<Item> T2_SPECIALIZED_VIRUS = ITEMS.singleStack("t2_specialized_virus");
    public static final DeferredItem<Item> T2_DEDICATED_VIRUS = ITEMS.singleStack("t2_dedicated_virus");
    public static final DeferredItem<Item> T3_RULE_VIRUS = ITEMS.singleStack("t3_rule_virus");
    public static final DeferredItem<Item> CREATIVE_VIRUS = ITEMS.singleStack("creative_virus");

    public static final DeferredItem<Item> DATA_FRAGMENT = ITEMS.material("data_fragment");
    public static final DeferredItem<Item> DAMAGED_DATA_PACKET = ITEMS.material("damaged_data_packet");
    public static final DeferredItem<Item> CONTAMINATED_ITEM_INDEX = ITEMS.material("contaminated_item_index");
    public static final DeferredItem<Item> DATA_STREAM_SAMPLE = ITEMS.material("data_stream_sample");
    public static final DeferredItem<Item> DATA_STREAM_CAPSULE = ITEMS.singleStack("data_stream_capsule");
    public static final DeferredItem<Item> DATA_STREAM_STORAGE_CELL = ITEMS.singleStack("data_stream_storage_cell");

    public static final DeferredItem<Item> ANTIVIRUS_MODULE = ITEMS.material("antivirus_module");
    public static final DeferredItem<Item> SECURITY_DATABASE = ITEMS.material("security_database");
    public static final DeferredItem<Item> SCAN_ACCELERATION_CARD = ITEMS.material("scan_acceleration_card");
    public static final DeferredItem<Item> ANTIVIRUS_ENHANCEMENT_CARD = ITEMS.material("antivirus_enhancement_card");
    public static final DeferredItem<Item> REPAIR_ENHANCEMENT_CARD = ITEMS.material("repair_enhancement_card");
    public static final DeferredItem<Item> LOG_MODULE = ITEMS.material("log_module");
    public static final DeferredItem<Item> ISOLATION_MODULE = ITEMS.material("isolation_module");
    public static final DeferredItem<Item> AUTO_SCAN_MODULE = ITEMS.material("auto_scan_module");
    public static final DeferredItem<Item> AUTO_ANTIVIRUS_MODULE = ITEMS.material("auto_antivirus_module");
    public static final DeferredItem<Item> SAMPLE_RECOVERY_MODULE = ITEMS.material("sample_recovery_module");

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
