package com.java.beipuo.ae2virus.machine;

import appeng.api.config.Actionable;
import appeng.api.inventories.InternalInventory;
import appeng.api.config.PowerMultiplier;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.orientation.BlockOrientation;
import appeng.api.util.AECableType;
import appeng.blockentity.grid.AENetworkedPoweredBlockEntity;
import com.java.beipuo.ae2virus.item.DataStreamCapsuleItem;
import com.java.beipuo.ae2virus.recipe.VirusMachineInput;
import com.java.beipuo.ae2virus.registry.AVBlockEntities;
import com.java.beipuo.ae2virus.registry.AVBlocks;
import com.java.beipuo.ae2virus.registry.AVItems;
import com.java.beipuo.ae2virus.storage.DataStreamKey;
import com.java.beipuo.ae2virus.storage.DataStreamKeyType;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class VirusMachineBlockEntity extends AENetworkedPoweredBlockEntity implements MenuProvider {
    private static final int MAX_ENERGY = 10000;
    private static final int DEFAULT_MAX_PROGRESS = 100;

    private final VirusMachineKind kind;
    private final VirusMachineInventory inventory = new VirusMachineInventory(this);
    private int progress;
    private int maxProgress = DEFAULT_MAX_PROGRESS;
    private final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> VirusMachineBlockEntity.this.progress;
                case 1 -> VirusMachineBlockEntity.this.maxProgress;
                case 2 -> (int) Math.floor(VirusMachineBlockEntity.this.getInternalCurrentPower());
                case 3 -> MAX_ENERGY;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> VirusMachineBlockEntity.this.progress = value;
                case 1 -> VirusMachineBlockEntity.this.maxProgress = value;
                case 2 -> VirusMachineBlockEntity.this.setInternalCurrentPower(value);
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public VirusMachineBlockEntity(BlockPos pos, BlockState state) {
        this(resolveKind(state), pos, state);
    }

    public VirusMachineBlockEntity(VirusMachineKind kind, BlockPos pos, BlockState state) {
        super(AVBlockEntities.VIRUS_MACHINE.get(), pos, state);
        this.kind = kind;
        this.getMainNode().setIdlePowerUsage(0);
        this.setInternalMaxPower(MAX_ENERGY);
        this.setPowerSides(getGridConnectableSides(getOrientation()));
    }

    private static VirusMachineKind resolveKind(BlockState state) {
        if (state.getBlock() == AVBlocks.VIRUS_ASSEMBLER.get()) {
            return VirusMachineKind.ASSEMBLER;
        }
        if (state.getBlock() == AVBlocks.VIRUS_BREEDER.get()) {
            return VirusMachineKind.BREEDER;
        }
        return VirusMachineKind.CORE_MANUFACTURER;
    }

    public VirusMachineKind kind() {
        return this.kind;
    }

    public VirusMachineInventory inventory() {
        return this.inventory;
    }

    public int energy() {
        return (int) Math.floor(this.getInternalCurrentPower());
    }

    public int progress() {
        return this.progress;
    }

    public int maxProgress() {
        return this.maxProgress;
    }

    public int maxEnergy() {
        return MAX_ENERGY;
    }

    public ContainerData containerData() {
        return this.containerData;
    }

    @Override
    public InternalInventory getInternalInventory() {
        return this.inventory;
    }

    @Nullable
    HolderLookup.Provider registryAccess() {
        return this.level == null ? null : this.level.registryAccess();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, VirusMachineBlockEntity machine) {
        if (level.isClientSide()) {
            return;
        }
        if (!machine.tryProcess((ServerLevel) level)) {
            machine.progress = 0;
        }
    }

    private boolean tryProcess(ServerLevel level) {
        var recipe = findRecipe(level);
        if (recipe == null) {
            return false;
        }
        ItemStack result = recipe.value().assemble(createRecipeInput(level.registryAccess()), level.registryAccess());
        if (result.isEmpty() || !canInsertOutput(result)) {
            return false;
        }

        double powerPerTick = (double) this.kind.energyCost() / this.maxProgress;
        IEnergySource energySource = selectEnergySource(powerPerTick);
        if (energySource == null) {
            return false;
        }
        energySource.extractAEPower(powerPerTick, Actionable.MODULATE, PowerMultiplier.CONFIG);
        this.progress++;
        if (this.progress < this.maxProgress) {
            setChanged();
            return true;
        }
        this.progress = 0;
        consumeInputs(level.registryAccess());
        insertOutput(result);
        setChanged();
        return true;
    }

    @Nullable
    private IEnergySource selectEnergySource(double amount) {
        double threshold = amount - 0.01;
        double internal = this.extractAEPower(amount, Actionable.SIMULATE, PowerMultiplier.CONFIG);
        if (internal > threshold) {
            return this;
        }
        var grid = this.getMainNode().getGrid();
        if (grid == null) {
            return null;
        }
        var energyService = grid.getEnergyService();
        double network = energyService.extractAEPower(amount, Actionable.SIMULATE, PowerMultiplier.CONFIG);
        return network > threshold ? energyService : null;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private RecipeHolder<? extends Recipe<VirusMachineInput>> findRecipe(ServerLevel level) {
        VirusMachineInput input = createRecipeInput(level.registryAccess());
        var recipeType = (RecipeType<Recipe<VirusMachineInput>>) this.kind.recipeType();
        for (RecipeHolder<Recipe<VirusMachineInput>> holder : level.getRecipeManager().getAllRecipesFor(recipeType)) {
            Recipe<VirusMachineInput> recipe = holder.value();
            if (recipe.matches(input, level)) {
                return holder;
            }
        }
        return null;
    }

    private VirusMachineInput createRecipeInput(HolderLookup.Provider registries) {
        List<ItemStack> items = new ArrayList<>(this.kind.itemInputSlots());
        for (int slot = 0; slot < this.kind.itemInputSlots(); slot++) {
            items.add(this.inventory.getStackInSlot(slot));
        }
        ItemStack dataStack = this.inventory.getStackInSlot(VirusMachineInventory.SLOT_DATA_STREAM);
        DataStreamKey dataStream = DataStreamCapsuleItem.getDataStream(dataStack, registries);
        long amount = dataStream == null ? 0L : DataStreamKeyType.AMOUNT_MB;
        return new VirusMachineInput(items, dataStream, amount);
    }

    private void consumeInputs(HolderLookup.Provider registries) {
        for (int slot = 0; slot < this.kind.itemInputSlots(); slot++) {
            this.inventory.extractItem(slot, 1, false);
        }
        ItemStack dataStack = this.inventory.getStackInSlot(VirusMachineInventory.SLOT_DATA_STREAM);
        if (!dataStack.isEmpty()) {
            DataStreamCapsuleItem.setDataStream(dataStack, registries, null);
            this.inventory.setItemDirect(VirusMachineInventory.SLOT_DATA_STREAM, dataStack);
        }
    }

    private boolean canInsertOutput(ItemStack result) {
        ItemStack output = this.inventory.getStackInSlot(VirusMachineInventory.SLOT_OUTPUT);
        return output.isEmpty() || (ItemStack.isSameItemSameComponents(output, result)
                && output.getCount() + result.getCount() <= output.getMaxStackSize());
    }

    private void insertOutput(ItemStack result) {
        ItemStack output = this.inventory.getStackInSlot(VirusMachineInventory.SLOT_OUTPUT);
        if (output.isEmpty()) {
            this.inventory.setItemDirect(VirusMachineInventory.SLOT_OUTPUT, result.copy());
        } else {
            output.grow(result.getCount());
            this.inventory.setItemDirect(VirusMachineInventory.SLOT_OUTPUT, output);
        }
    }

    @Override
    public Component getDisplayName() {
        return this.kind.title();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new VirusMachineMenu(id, playerInventory, this);
    }

    public List<ItemStack> drops() {
        List<ItemStack> drops = new ArrayList<>();
        for (int slot = 0; slot < this.inventory.size(); slot++) {
            ItemStack stack = this.inventory.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                drops.add(stack.copy());
            }
        }
        return drops;
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("progress", this.progress);
        tag.putInt("max_progress", this.maxProgress);
    }

    @Override
    public void loadTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadTag(tag, registries);
        this.progress = tag.getInt("progress");
        this.maxProgress = tag.getInt("max_progress");
        if (this.maxProgress <= 0) {
            this.maxProgress = DEFAULT_MAX_PROGRESS;
        }
    }

    @Override
    public EnumSet<Direction> getGridConnectableSides(BlockOrientation orientation) {
        return EnumSet.allOf(Direction.class);
    }

    @Override
    public AECableType getCableConnectionType(Direction dir) {
        return AECableType.COVERED;
    }

    public ItemStack getMainMenuIcon() {
        return switch (this.kind) {
            case ASSEMBLER -> AVItems.VIRUS_ASSEMBLER.toStack();
            case BREEDER -> AVItems.VIRUS_BREEDER.toStack();
            case CORE_MANUFACTURER -> AVItems.VIRUS_CORE_MANUFACTURER.toStack();
        };
    }
}
