package com.java.beipuo.ae2virus.infection;

import appeng.api.implementations.parts.ICablePart;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridServiceProvider;
import appeng.api.networking.storage.IStorageService;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.util.AECableType;
import appeng.parts.AEBasePart;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class VirusNetworkService implements IVirusNetworkService, IGridServiceProvider {
    private static final String TAG_T1_VIRUSES = "ae2virus:t1_viruses";
    private static final Map<ResourceLocation, TickBudget> RISK_CHECK_BUDGETS = new HashMap<>();

    private final IGrid grid;
    private final IStorageService storageService;
    private final InfectionConfig config = InfectionConfig.defaults();
    private final RandomSource random = RandomSource.create();
    private final VirusNetworkRiskCache riskCache = new VirusNetworkRiskCache();
    private final List<T1VirusState> t1Viruses = new ArrayList<>();
    private final Set<IGridNode> trackedNodes = new HashSet<>();
    private VirusNetworkStats stats = VirusNetworkStats.EMPTY;
    private int ticksUntilRiskCheck;
    private int infectionVersion;

    public VirusNetworkService(IGrid grid, IStorageService storageService) {
        this.grid = grid;
        this.storageService = storageService;
        VirusNetworkStorageGuards.register(storageService.getInventory(), this);
        this.ticksUntilRiskCheck = 20
                + this.random.nextInt(Math.max(1, this.config.runtime().riskCheckIntervalTicks()));
    }

    @Override
    public void onLevelEndTick(Level level) {
        if (this.trackedNodes.isEmpty() || !isNetworkInLevel(level)) {
            return;
        }

        if (this.ticksUntilRiskCheck > 0) {
            this.ticksUntilRiskCheck--;
            return;
        }

        if (!claimRiskCheckBudget(level)) {
            return;
        }

        refreshStats();
        runT1RiskCheck();
        this.ticksUntilRiskCheck = nextRiskCheckInterval();
    }

    @Override
    public void addNode(IGridNode gridNode, CompoundTag savedData) {
        this.trackedNodes.add(gridNode);
        this.riskCache.markExposureDirty();
        restoreT1Viruses(gridNode, savedData);
    }

    @Override
    public void removeNode(IGridNode gridNode) {
        this.trackedNodes.remove(gridNode);
        this.riskCache.markExposureDirty();
    }

    @Override
    public VirusNetworkStats getStats() {
        return this.stats;
    }

    @Override
    public List<T1VirusState> t1Viruses() {
        return List.copyOf(this.t1Viruses);
    }

    @Override
    public long blockedAmount(AEKey key) {
        long blocked = 0L;
        for (T1VirusState virus : this.t1Viruses) {
            if (virus.target().equals(key)) {
                blocked = saturatedAdd(blocked, virus.blockedAmount());
            }
        }
        return blocked;
    }

    @Override
    public long allowedExtraction(AEKey key, long requestedAmount) {
        refreshStats();
        long storedAmount = storedAmount(key);
        long blockedAmount = Math.min(storedAmount, blockedAmount(key));
        long extractableAmount = Math.max(0L, storedAmount - blockedAmount);
        return Math.min(requestedAmount, extractableAmount);
    }

    @Override
    public void saveNodeData(IGridNode gridNode, CompoundTag savedData) {
        if (this.t1Viruses.isEmpty()) {
            savedData.remove(TAG_T1_VIRUSES);
            return;
        }

        ListTag viruses = new ListTag();
        for (T1VirusState virus : this.t1Viruses) {
            CompoundTag tag = new CompoundTag();
            tag.put("target", virus.target().toTagGeneric(gridNode.getLevel().registryAccess()));
            tag.putLong("blockedAmount", virus.blockedAmount());
            tag.putLong("experience", virus.experience());
            viruses.add(tag);
        }
        savedData.put(TAG_T1_VIRUSES, viruses);
    }

    @Override
    public int getInfectionVersion() {
        return this.infectionVersion;
    }

    private void refreshStats() {
        this.riskCache.rebuildStorage(this.storageService.getCachedInventory(),
                this.config.runtime().maxCandidatesPerRefresh());
        if (this.riskCache.exposureDirty()) {
            this.riskCache.setExposureStats(rebuildExposureStats());
        }

        int storedKeyCount = 0;
        long totalStoredAmount = 0L;
        List<AEKey> itemCandidates = new ArrayList<>();
        for (var entry : this.storageService.getCachedInventory()) {
            AEKey key = entry.getKey();
            long amount = entry.getLongValue();
            if (amount <= 0) {
                continue;
            }

            storedKeyCount++;
            totalStoredAmount = saturatedAdd(totalStoredAmount, amount);
            if (key instanceof AEItemKey && itemCandidates.size() < this.config.runtime().maxCandidatesPerRefresh()) {
                itemCandidates.add(key);
            }
        }

        this.stats = new VirusNetworkStats(storedKeyCount, totalStoredAmount, List.copyOf(itemCandidates));
    }

    private void runT1RiskCheck() {
        AEKey target = randomT1Candidate();
        if (target == null) {
            return;
        }

        if (!canT1Infect(target)) {
            return;
        }

        InfectionRoll roll = InfectionRisk.roll(
                InfectionRisk.t1AttemptChance(storedAmount(target), this.config),
                this.riskCache.exposureStats(),
                this.random,
                this.config);
        if (roll.result() != InfectionRoll.Result.SUCCESS) {
            return;
        }

        T1VirusState virus = t1Virus(target);
        if (virus == null) {
            this.t1Viruses.add(new T1VirusState(target, 1L, 1L));
            markInfectionChanged();
        } else if (growT1Virus(virus)) {
            markInfectionChanged();
        }
    }

    private AEKey randomT1Candidate() {
        List<AEKey> candidates = this.riskCache.itemCandidates();
        if (candidates.isEmpty()) {
            return null;
        }
        for (int attempts = 0; attempts < candidates.size(); attempts++) {
            AEKey candidate = candidates.get(this.random.nextInt(candidates.size()));
            if (canT1Infect(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private T1VirusState t1Virus(AEKey target) {
        for (T1VirusState virus : this.t1Viruses) {
            if (virus.target().equals(target)) {
                return virus;
            }
        }
        return null;
    }

    private boolean growT1Virus(T1VirusState virus) {
        long storedAmount = storedAmount(virus.target());
        if (virus.blockedAmount() >= storedAmount) {
            return false;
        }
        long room = storedAmount - virus.blockedAmount();
        long growth = InfectionRisk.spreadGrowthAmount(virus.blockedAmount(), virus.level(), this.config);
        return virus.addBlockedAmount(Math.min(room, growth));
    }

    private static boolean canT1Infect(AEKey key) {
        return key instanceof AEItemKey itemKey && itemKey.getItem() != Items.NETHER_STAR;
    }

    private long storedAmount(AEKey key) {
        if (key instanceof AEItemKey) {
            return this.riskCache.keyCount(key);
        }
        return 0L;
    }

    private int nextRiskCheckInterval() {
        int baseInterval = this.config.runtime().riskCheckIntervalTicks();
        long infectedAmount = t1InfectedAmount();
        int virusLevel = highestT1Level();
        int spreadInterval = InfectionRisk.spreadIntervalTicks(baseInterval, infectedAmount, virusLevel, this.config);
        return spreadInterval + this.random.nextInt(Math.max(1, spreadInterval / 4));
    }

    private long t1InfectedAmount() {
        long infectedAmount = 0L;
        for (T1VirusState virus : this.t1Viruses) {
            infectedAmount = saturatedAdd(infectedAmount, virus.blockedAmount());
        }
        return infectedAmount;
    }

    private int highestT1Level() {
        int level = 1;
        for (T1VirusState virus : this.t1Viruses) {
            level = Math.max(level, virus.level());
        }
        return level;
    }

    private void restoreT1Viruses(IGridNode gridNode, CompoundTag savedData) {
        if (savedData == null || !savedData.contains(TAG_T1_VIRUSES, Tag.TAG_LIST)) {
            return;
        }

        ListTag viruses = savedData.getList(TAG_T1_VIRUSES, Tag.TAG_COMPOUND);
        boolean changed = false;
        for (int i = 0; i < viruses.size(); i++) {
            CompoundTag virusTag = viruses.getCompound(i);
            AEKey target = AEKey.fromTagGeneric(gridNode.getLevel().registryAccess(), virusTag.getCompound("target"));
            if (target == null) {
                continue;
            }
            long blockedAmount = virusTag.getLong("blockedAmount");
            long experience = virusTag.getLong("experience");
            T1VirusState existing = t1Virus(target);
            if (existing == null) {
                this.t1Viruses.add(new T1VirusState(target, blockedAmount, experience));
                changed = true;
            } else if (existing.merge(blockedAmount, experience)) {
                changed = true;
            }
        }
        if (changed) {
            this.infectionVersion++;
        }
    }

    private void markInfectionChanged() {
        this.infectionVersion++;
        markTrackedNodesForSave();
    }

    private void markTrackedNodesForSave() {
        for (IGridNode node : this.trackedNodes) {
            Object owner = node.getOwner();
            if (owner instanceof AEBasePart part) {
                part.getHost().markForSave();
            } else if (owner instanceof BlockEntity blockEntity) {
                blockEntity.setChanged();
            }
        }
    }

    private ExposureStats rebuildExposureStats() {
        int exposedCableFaces = 0;
        double machineExposureWeight = 0.0;

        for (IGridNode node : this.trackedNodes) {
            Object owner = node.getOwner();
            if (owner instanceof AEBasePart part) {
                BlockEntity blockEntity = part.getBlockEntity();
                if (blockEntity != null) {
                    if (owner instanceof ICablePart cablePart
                            && cablePart.getCableConnectionType() == AECableType.GLASS) {
                        exposedCableFaces += countAirFaces(blockEntity.getLevel(), blockEntity.getBlockPos());
                    } else {
                        machineExposureWeight += countMachineExposure(blockEntity.getLevel(), blockEntity.getBlockPos());
                    }
                }
            } else if (owner instanceof BlockEntity blockEntity) {
                machineExposureWeight += countMachineExposure(blockEntity.getLevel(), blockEntity.getBlockPos());
            }
        }

        return new ExposureStats(exposedCableFaces, 0, machineExposureWeight);
    }

    private static int countAirFaces(Level level, BlockPos pos) {
        if (level == null) {
            return 0;
        }

        int exposedFaces = 0;
        for (Direction direction : Direction.values()) {
            if (level.getBlockState(pos.relative(direction)).isAir()) {
                exposedFaces++;
            }
        }
        return exposedFaces;
    }

    private double countMachineExposure(Level level, BlockPos pos) {
        return countAirFaces(level, pos) * this.config.exposure().machineFaceWeight();
    }

    private boolean isNetworkInLevel(Level level) {
        for (IGridNode node : this.trackedNodes) {
            if (node.getLevel() == level) {
                return true;
            }
        }
        return false;
    }

    private boolean claimRiskCheckBudget(Level level) {
        ResourceLocation levelId = level.dimension().location();
        long gameTime = level.getGameTime();
        TickBudget budget = RISK_CHECK_BUDGETS.get(levelId);
        if (budget == null || budget.gameTime != gameTime) {
            budget = new TickBudget(gameTime);
            RISK_CHECK_BUDGETS.put(levelId, budget);
        }
        if (budget.used >= this.config.runtime().maxRiskChecksPerTick()) {
            return false;
        }
        budget.used++;
        return true;
    }

    private static long saturatedAdd(long left, long right) {
        long result = left + right;
        return result < 0 ? Long.MAX_VALUE : result;
    }

    private static final class TickBudget {
        private final long gameTime;
        private int used;

        private TickBudget(long gameTime) {
            this.gameTime = gameTime;
        }
    }
}
