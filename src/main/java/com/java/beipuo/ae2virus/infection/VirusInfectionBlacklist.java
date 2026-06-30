package com.java.beipuo.ae2virus.infection;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import com.java.beipuo.ae2virus.item.DataStreamCapsuleItem;
import com.java.beipuo.ae2virus.item.DataStreamStorageCellItem;
import java.util.Set;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public final class VirusInfectionBlacklist {
    private static final Set<Item> BLACKLISTED_ITEMS = Set.of(
            Items.NETHER_STAR,
            Items.DRAGON_EGG,
            Items.HEART_OF_THE_SEA,
            Items.ELYTRA,
            Items.HEAVY_CORE);

    private VirusInfectionBlacklist() {
    }

    public static boolean contains(AEKey key) {
        return key instanceof AEItemKey itemKey && contains(itemKey.getItem());
    }

    public static boolean contains(Item item) {
        return BLACKLISTED_ITEMS.contains(item)
                || item instanceof DataStreamCapsuleItem
                || item instanceof DataStreamStorageCellItem;
    }

    public static boolean canGenericVirusInfect(AEKey key) {
        return key instanceof AEItemKey && !contains(key);
    }
}
