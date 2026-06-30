package com.java.beipuo.ae2virus.item;

import appeng.api.config.FuzzyMode;
import appeng.api.ids.AEComponents;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.storage.cells.IBasicCellItem;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.UpgradeInventories;
import appeng.items.contents.CellConfig;
import appeng.util.ConfigInventory;
import com.java.beipuo.ae2virus.storage.DataStreamKey;
import com.java.beipuo.ae2virus.storage.DataStreamKeyType;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class DataStreamStorageCellItem extends Item implements IBasicCellItem {
    private static final int TOTAL_BYTES = 64 * 1024;
    private static final int BYTES_PER_TYPE = 8;
    private static final int TOTAL_TYPES = 63;
    private static final double IDLE_DRAIN = 1.0;

    public DataStreamStorageCellItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip,
            TooltipFlag flag) {
        addCellInformationToTooltip(stack, tooltip);
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        return getCellTooltipImage(stack);
    }

    @Override
    public AEKeyType getKeyType() {
        return DataStreamKeyType.TYPE;
    }

    @Override
    public int getBytes(ItemStack cellItem) {
        return TOTAL_BYTES;
    }

    @Override
    public int getBytesPerType(ItemStack cellItem) {
        return BYTES_PER_TYPE;
    }

    @Override
    public int getTotalTypes(ItemStack cellItem) {
        return TOTAL_TYPES;
    }

    @Override
    public boolean isBlackListed(ItemStack cellItem, AEKey requestedAddition) {
        return !(requestedAddition instanceof DataStreamKey);
    }

    @Override
    public boolean storableInStorageCell() {
        return false;
    }

    @Override
    public double getIdleDrain() {
        return IDLE_DRAIN;
    }

    @Override
    public IUpgradeInventory getUpgrades(ItemStack is) {
        return UpgradeInventories.forItem(is, 4);
    }

    @Override
    public ConfigInventory getConfigInventory(ItemStack is) {
        return CellConfig.create(Set.of(DataStreamKeyType.TYPE), is);
    }

    @Override
    public FuzzyMode getFuzzyMode(ItemStack is) {
        return is.getOrDefault(AEComponents.STORAGE_CELL_FUZZY_MODE, FuzzyMode.IGNORE_ALL);
    }

    @Override
    public void setFuzzyMode(ItemStack is, FuzzyMode fuzzyMode) {
        is.set(AEComponents.STORAGE_CELL_FUZZY_MODE, fuzzyMode);
    }
}
