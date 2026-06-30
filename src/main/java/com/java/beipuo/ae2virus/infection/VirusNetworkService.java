package com.java.beipuo.ae2virus.infection;

import appeng.api.config.Actionable;
import appeng.api.implementations.blockentities.IChestOrDrive;
import appeng.api.implementations.parts.ICablePart;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridServiceProvider;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageService;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.storage.cells.StorageCell;
import appeng.api.util.AECableType;
import com.java.beipuo.ae2virus.item.DataStreamStorageCellItem;
import com.java.beipuo.ae2virus.registry.AVItems;
import com.java.beipuo.ae2virus.storage.DataStreamKey;
import com.java.beipuo.ae2virus.storage.DataStreamKeyType;
import appeng.parts.AEBasePart;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class VirusNetworkService implements IVirusNetworkService, IGridServiceProvider {
    private static final String TAG_T1_VIRUSES = "ae2virus:t1_viruses";
    private static final String TAG_T2_VIRUSES = "ae2virus:t2_viruses";
    private static final String TAG_T3_VIRUSES = "ae2virus:t3_viruses";
    private static final Map<ResourceLocation, TickBudget> RISK_CHECK_BUDGETS = new HashMap<>();

    private final IGrid grid;
    private final IStorageService storageService;
    private final InfectionConfig config = InfectionConfig.defaults();
    private final RandomSource random = RandomSource.create();
    private final IActionSource actionSource = IActionSource.empty();
    private final VirusNetworkRiskCache riskCache = new VirusNetworkRiskCache();
    private final List<T1VirusState> t1Viruses = new ArrayList<>();
    private final List<T2VirusState> t2Viruses = new ArrayList<>();
    private final List<T3VirusState> t3Viruses = new ArrayList<>();
    private final Set<IGridNode> trackedNodes = new HashSet<>();
    private Map<String, T3CellInfo> t3Cells = Map.of();
    private VirusNetworkStats stats = VirusNetworkStats.EMPTY;
    private int ticksUntilRiskCheck;
    private int stimulationTicks;
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

        boolean stimulated = isStimulated();

        if (this.ticksUntilRiskCheck > 0) {
            this.ticksUntilRiskCheck--;
            tickStimulation(stimulated);
            return;
        }

        if (!stimulated && !claimRiskCheckBudget(level)) {
            tickStimulation(false);
            return;
        }

        refreshStats();
        runT1RiskCheck();
        runT2RiskCheck();
        runT3RiskCheck();
        this.ticksUntilRiskCheck = nextRiskCheckInterval();
        tickStimulation(stimulated);
    }

    @Override
    public void addNode(IGridNode gridNode, CompoundTag savedData) {
        this.trackedNodes.add(gridNode);
        this.riskCache.markExposureDirty();
        restoreT1Viruses(gridNode, savedData);
        restoreT2Viruses(gridNode, savedData);
        restoreT3Viruses(gridNode, savedData);
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
    public List<T2VirusState> t2Viruses() {
        return List.copyOf(this.t2Viruses);
    }

    @Override
    public List<T3VirusState> t3Viruses() {
        return List.copyOf(this.t3Viruses);
    }

    @Override
    public long blockedAmount(AEKey key) {
        long blocked = 0L;
        for (T1VirusState virus : this.t1Viruses) {
            if (virus.target().equals(key)) {
                blocked = saturatedAdd(blocked, virus.blockedAmount());
            }
        }
        for (T2VirusState virus : this.t2Viruses) {
            blocked = saturatedAdd(blocked, virus.blockedAmount(key));
        }
        for (T3CellInfo cell : this.t3Cells.values()) {
            T3VirusState virus = t3Virus(cell.cellId());
            if (virus != null) {
                blocked = saturatedAdd(blocked, Math.min(cell.storedAmount(key), virus.blockedAmount(key)));
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
    public void stimulateViruses(int durationTicks) {
        this.stimulationTicks = Math.max(this.stimulationTicks, durationTicks);
        this.ticksUntilRiskCheck = 0;
    }

    @Override
    public void saveNodeData(IGridNode gridNode, CompoundTag savedData) {
        if (this.t1Viruses.isEmpty()) {
            savedData.remove(TAG_T1_VIRUSES);
        } else {
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

        saveT2Viruses(gridNode, savedData);
        saveT3Viruses(gridNode, savedData);
    }

    @Override
    public int getInfectionVersion() {
        return this.infectionVersion;
    }

    private void refreshStats() {
        this.riskCache.rebuildStorage(this.storageService.getCachedInventory(),
                this.config.runtime().maxCandidatesPerRefresh());
        this.t3Cells = t3CellsById();
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
                activeExposureStats(),
                this.random,
                this.config);
        if (roll.result() != InfectionRoll.Result.SUCCESS) {
            return;
        }

        T1VirusState virus = t1Virus(target);
        if (virus == null) {
            this.t1Viruses.add(new T1VirusState(target, 1L, 1L));
            markInfectionChanged();
            handleInfectionDrops(1L);
            dropDataStream(target, 1, null, 1);
        } else {
            long added = growT1Virus(virus);
            if (added <= 0L) {
                return;
            }
            markInfectionChanged();
            handleInfectionDrops(added);
            dropDataStream(target, 1, null, virus.level());
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

    private long growT1Virus(T1VirusState virus) {
        long storedAmount = storedAmount(virus.target());
        if (virus.blockedAmount() >= storedAmount) {
            return 0L;
        }
        long room = storedAmount - virus.blockedAmount();
        long growth = spreadGrowthAmount(virus.blockedAmount(), virus.level());
        long added = Math.min(room, growth);
        return virus.addBlockedAmount(added) ? added : 0L;
    }

    private static boolean canT1Infect(AEKey key) {
        return VirusInfectionBlacklist.canGenericVirusInfect(key);
    }

    private void runT2RiskCheck() {
        if (this.t1Viruses.isEmpty()) {
            return;
        }

        InfectionGrowth growth = null;
        if (this.random.nextBoolean()) {
            growth = runT2FusionRiskCheck();
            if (growth == null) {
                growth = runT2SpecializedRiskCheck();
            }
        } else {
            growth = runT2SpecializedRiskCheck();
            if (growth == null) {
                growth = runT2FusionRiskCheck();
            }
        }

        if (growth != null) {
            markInfectionChanged();
            handleInfectionDrops(growth.amount());
        }
    }

    private InfectionGrowth runT2FusionRiskCheck() {
        List<T1VirusState> seeds = t1SeedViruses();
        if (seeds.size() < 2) {
            return null;
        }

        T1VirusState seed = seeds.get(this.random.nextInt(seeds.size()));
        double conversionChance = InfectionRisk.t2FusionConversionChance(seeds.size(), seed.level(), this.config);
        if (this.random.nextDouble() >= conversionChance) {
            return null;
        }

        List<AEKey> fusionTargets = new ArrayList<>();
        for (T1VirusState virus : seeds) {
            fusionTargets.add(virus.target());
        }
        ResourceLocation targetId = T2VirusTargets.fusionTargetId(fusionTargets);
        T2VirusState virus = t2Virus(T2VirusKind.FUSION, targetId);
        AEKey target = randomT2FusionTarget(virus, fusionTargets);
        if (target == null) {
            return null;
        }

        T2VirusState targetVirus = virus == null ? new T2VirusState(T2VirusKind.FUSION, targetId, 1L) : virus;
        long added = growT2Virus(targetVirus, target);
        if (added <= 0L) {
            return null;
        }
        if (virus == null) {
            this.t2Viruses.add(targetVirus);
        }
        dropDataStream(target, 2, targetVirus.kind(), targetVirus.level());
        return new InfectionGrowth(target, added);
    }

    private InfectionGrowth runT2SpecializedRiskCheck() {
        T2SpecializedChoice choice = randomT2SpecializedChoice();
        if (choice == null) {
            return null;
        }

        double conversionChance = InfectionRisk.t2SpecializedConversionChance(choice.seedCount(), choice.seedLevel(),
                this.config);
        if (this.random.nextDouble() >= conversionChance) {
            return null;
        }

        T2VirusState virus = t2Virus(T2VirusKind.SPECIALIZED, choice.tagId());
        AEKey target = randomT2SpecializedTarget(virus, choice.tagId());
        if (target == null) {
            return null;
        }

        T2VirusState targetVirus = virus == null ? new T2VirusState(T2VirusKind.SPECIALIZED, choice.tagId(), 1L) : virus;
        long added = growT2Virus(targetVirus, target);
        if (added <= 0L) {
            return null;
        }
        if (virus == null) {
            this.t2Viruses.add(targetVirus);
        }
        dropDataStream(target, 2, targetVirus.kind(), targetVirus.level());
        return new InfectionGrowth(target, added);
    }

    private List<T1VirusState> t1SeedViruses() {
        List<T1VirusState> seeds = new ArrayList<>();
        for (T1VirusState virus : this.t1Viruses) {
            if (canT1Infect(virus.target()) && storedAmount(virus.target()) > 0) {
                seeds.add(virus);
            }
        }
        return seeds;
    }

    private AEKey randomT2FusionTarget(T2VirusState virus, List<AEKey> fusionTargets) {
        List<AEKey> candidates = new ArrayList<>();
        for (AEKey target : fusionTargets) {
            if (canGenericT2InfectTarget(target, virus)) {
                candidates.add(target);
            }
        }
        return randomCandidate(candidates);
    }

    private T2SpecializedChoice randomT2SpecializedChoice() {
        Map<ResourceLocation, TagSeedStats> tagStats = new HashMap<>();
        for (T1VirusState virus : t1SeedViruses()) {
            for (ResourceLocation tagId : T2VirusTargets.itemTagIds(virus.target())) {
                TagSeedStats stats = tagStats.computeIfAbsent(tagId, ignored -> new TagSeedStats());
                stats.seedCount++;
                stats.seedLevel = Math.max(stats.seedLevel, virus.level());
            }
        }

        List<T2SpecializedChoice> choices = new ArrayList<>();
        for (Map.Entry<ResourceLocation, TagSeedStats> entry : tagStats.entrySet()) {
            T2VirusState existing = t2Virus(T2VirusKind.SPECIALIZED, entry.getKey());
            int susceptibleCount = countT2SpecializedTargets(entry.getKey(), existing);
            if (entry.getValue().seedCount > 0 && susceptibleCount > 0) {
                choices.add(new T2SpecializedChoice(entry.getKey(), entry.getValue().seedCount,
                        susceptibleCount, entry.getValue().seedLevel));
            }
        }
        return weightedSpecializedChoice(choices);
    }

    private T2SpecializedChoice weightedSpecializedChoice(List<T2SpecializedChoice> choices) {
        int totalWeight = 0;
        for (T2SpecializedChoice choice : choices) {
            totalWeight += Math.max(1, choice.seedCount() * choice.susceptibleCount());
        }
        if (totalWeight <= 0) {
            return null;
        }

        int roll = this.random.nextInt(totalWeight);
        for (T2SpecializedChoice choice : choices) {
            roll -= Math.max(1, choice.seedCount() * choice.susceptibleCount());
            if (roll < 0) {
                return choice;
            }
        }
        return choices.get(choices.size() - 1);
    }

    private int countT2SpecializedTargets(ResourceLocation tagId, T2VirusState virus) {
        int count = 0;
        for (AEKey candidate : this.riskCache.itemCandidates()) {
            if (T2VirusTargets.hasTag(candidate, tagId) && canGenericT2InfectTarget(candidate, virus)) {
                count++;
            }
        }
        return count;
    }

    private AEKey randomT2SpecializedTarget(T2VirusState virus, ResourceLocation tagId) {
        List<AEKey> candidates = new ArrayList<>();
        for (AEKey candidate : this.riskCache.itemCandidates()) {
            if (T2VirusTargets.hasTag(candidate, tagId) && canGenericT2InfectTarget(candidate, virus)) {
                candidates.add(candidate);
            }
        }
        return randomCandidate(candidates);
    }

    private boolean canGenericT2InfectTarget(AEKey target, T2VirusState virus) {
        long blockedAmount = virus == null ? 0L : virus.blockedAmount(target);
        return target instanceof AEItemKey
                && !VirusInfectionBlacklist.contains(target)
                && storedAmount(target) > blockedAmount;
    }

    private long growT2Virus(T2VirusState virus, AEKey target) {
        long storedAmount = storedAmount(target);
        long currentBlocked = virus.blockedAmount(target);
        if (currentBlocked >= storedAmount) {
            return 0L;
        }

        long room = storedAmount - currentBlocked;
        long growth = spreadGrowthAmount(Math.max(1L, virus.totalBlockedAmount()), virus.level());
        InfectionRoll roll = InfectionRisk.roll(
                InfectionRisk.t2InfectionAttemptChance(Math.max(1, virus.infectedTargetCount() + 1), this.config),
                activeExposureStats(),
                this.random,
                this.config);
        if (roll.result() != InfectionRoll.Result.SUCCESS) {
            return 0L;
        }

        long added = Math.min(room, growth);
        return virus.addBlockedAmount(target, added) ? added : 0L;
    }

    private void runT3RiskCheck() {
        Map<String, T3CellInfo> cells = this.t3Cells;
        if (cells.isEmpty()) {
            return;
        }

        InfectionGrowth growth = growExistingT3Virus(cells);
        if (growth == null && !this.t2Viruses.isEmpty()) {
            growth = runT3ConversionRiskCheck(cells);
        }

        if (growth != null) {
            markInfectionChanged();
            handleInfectionDrops(growth.amount());
        }
    }

    private void handleInfectionDrops(long newlyInfectedAmount) {
        dropVirusShells(newlyInfectedAmount);
    }

    private void dropVirusShells(long newlyInfectedAmount) {
        if (newlyInfectedAmount <= 0L) {
            return;
        }

        long shellAmount = Math.max(1L, newlyInfectedAmount / 100L);
        insertIntoNetwork(AEItemKey.of(AVItems.VIRUS_SHELL.get()), shellAmount);
    }

    private void dropDataStream(AEKey target, int tier, T2VirusKind t2Kind, int level) {
        if (!hasDataStreamStorageCell() || this.random.nextDouble() >= 0.01) {
            return;
        }
        insertDataStream(target, 1L, tier, t2Kind, level);
    }

    private boolean hasDataStreamStorageCell() {
        for (IGridNode node : this.trackedNodes) {
            Object owner = node.getOwner();
            if (!(owner instanceof BlockEntity blockEntity) || !(owner instanceof IChestOrDrive drive)
                    || blockEntity.getLevel() == null || !drive.isPowered()) {
                continue;
            }

            for (int slot = 0; slot < drive.getCellCount(); slot++) {
                if (drive.getCellItem(slot) instanceof DataStreamStorageCellItem) {
                    return true;
                }
            }
        }
        return false;
    }

    private void insertIntoNetwork(AEItemKey key, long amount) {
        this.storageService.getInventory().insert(key, amount, Actionable.MODULATE, this.actionSource);
    }

    private void insertDataStream(AEKey target, long amount, int tier, T2VirusKind t2Kind, int level) {
        DataStreamKey key = DataStreamKey.of(target, tier, t2Kind, level);
        if (key == null || amount <= 0L) {
            return;
        }
        long internalAmount = dataStreamAmount(amount);
        this.storageService.getInventory().insert(key, internalAmount, Actionable.MODULATE, this.actionSource);
    }

    private static long dataStreamAmount(long megabytes) {
        if (megabytes > Long.MAX_VALUE / DataStreamKeyType.AMOUNT_MB) {
            return Long.MAX_VALUE;
        }
        return megabytes * DataStreamKeyType.AMOUNT_MB;
    }

    private InfectionGrowth growExistingT3Virus(Map<String, T3CellInfo> cells) {
        List<T3VirusState> candidates = new ArrayList<>();
        for (T3VirusState virus : this.t3Viruses) {
            T3CellInfo cell = cells.get(virus.cellId());
            if (cell != null && !t3SusceptibleTargets(cell, virus).isEmpty()) {
                candidates.add(virus);
            }
        }

        T3VirusState virus = randomT3Virus(candidates);
        if (virus == null) {
            return null;
        }
        InfectionGrowth growth = growT3Virus(virus, cells.get(virus.cellId()));
        if (growth != null) {
            dropDataStream(growth.target(), 3, null, virus.level());
        }
        return growth;
    }

    private InfectionGrowth runT3ConversionRiskCheck(Map<String, T3CellInfo> cells) {
        double conversionChance = InfectionRisk.t3ConversionChance(t3ConversionRisk(), this.config);
        if (this.random.nextDouble() >= conversionChance) {
            return null;
        }

        T3CellInfo cell = randomT3Cell(cells);
        if (cell == null) {
            return null;
        }

        T3VirusState virus = t3Virus(cell.cellId());
        T3VirusState targetVirus = virus == null ? new T3VirusState(cell.cellId(), 1L) : virus;
        InfectionGrowth growth = growT3Virus(targetVirus, cell);
        if (growth == null) {
            return null;
        }
        if (virus == null) {
            this.t3Viruses.add(targetVirus);
        }
        dropDataStream(growth.target(), 3, null, targetVirus.level());
        return growth;
    }

    private double t3ConversionRisk() {
        long fusionCount = 0L;
        double fusionLevel = 1.0;
        long specializedCount = 0L;
        double specializedLevel = 1.0;
        for (T2VirusState virus : this.t2Viruses) {
            double level = Math.max(1, virus.level());
            if (virus.kind() == T2VirusKind.FUSION) {
                fusionCount++;
                fusionLevel = Math.max(fusionLevel, level);
            } else if (virus.kind() == T2VirusKind.SPECIALIZED) {
                specializedCount++;
                specializedLevel = Math.max(specializedLevel, level);
            }
        }

        return InfectionRisk.t3ConversionRisk(fusionCount, fusionLevel, specializedCount, specializedLevel,
                0L, 1.0, 0L, 1.0, 0L, 1.0, 0L, 1.0, 0L, 1.0, this.config);
    }

    private InfectionGrowth growT3Virus(T3VirusState virus, T3CellInfo cell) {
        AEKey target = randomCandidate(t3SusceptibleTargets(cell, virus));
        if (target == null) {
            return null;
        }

        long storedAmount = cell.storedAmount(target);
        long currentBlocked = virus.blockedAmount(target);
        if (currentBlocked >= storedAmount) {
            return null;
        }

        long room = storedAmount - currentBlocked;
        long growth = spreadGrowthAmount(Math.max(1L, virus.totalBlockedAmount()), virus.level());
        InfectionRoll roll = InfectionRisk.roll(
                InfectionRisk.t2InfectionAttemptChance(Math.max(1, virus.infectedTargetCount() + 1), this.config),
                activeExposureStats(),
                this.random,
                this.config);
        if (roll.result() != InfectionRoll.Result.SUCCESS) {
            return null;
        }

        long added = Math.min(room, growth);
        return virus.addBlockedAmount(target, added) ? new InfectionGrowth(target, added) : null;
    }

    private List<AEKey> t3SusceptibleTargets(T3CellInfo cell, T3VirusState virus) {
        List<AEKey> candidates = new ArrayList<>();
        for (Map.Entry<AEKey, Long> entry : cell.storedAmounts().entrySet()) {
            AEKey target = entry.getKey();
            if (target instanceof AEItemKey itemKey
                    && !(itemKey.getItem() instanceof DataStreamStorageCellItem)
                    && entry.getValue() > virus.blockedAmount(target)) {
                candidates.add(target);
            }
        }
        return candidates;
    }

    private T3CellInfo randomT3Cell(Map<String, T3CellInfo> cells) {
        List<T3CellInfo> candidates = new ArrayList<>();
        long totalWeight = 0L;
        for (T3CellInfo cell : cells.values()) {
            T3VirusState virus = t3Virus(cell.cellId());
            if (virus == null || !t3SusceptibleTargets(cell, virus).isEmpty()) {
                candidates.add(cell);
                totalWeight = saturatedAdd(totalWeight, t3CellWeight(cell, virus));
            }
        }
        if (candidates.isEmpty() || totalWeight <= 0L) {
            return null;
        }

        long roll = nextLong(totalWeight);
        for (T3CellInfo cell : candidates) {
            roll -= t3CellWeight(cell, t3Virus(cell.cellId()));
            if (roll < 0L) {
                return cell;
            }
        }
        return candidates.get(candidates.size() - 1);
    }

    private long t3CellWeight(T3CellInfo cell, T3VirusState virus) {
        int susceptibleTargets = virus == null ? cell.storedAmounts().size() : t3SusceptibleTargets(cell, virus).size();
        long storagePressure = Math.max(1L, cell.usedAmount() / 1024L);
        return Math.max(1L, saturatedAdd(storagePressure, susceptibleTargets));
    }

    private T3VirusState t3Virus(String cellId) {
        for (T3VirusState virus : this.t3Viruses) {
            if (virus.cellId().equals(cellId)) {
                return virus;
            }
        }
        return null;
    }

    private Map<String, T3CellInfo> t3CellsById() {
        Map<String, T3CellInfo> cells = new LinkedHashMap<>();
        for (IGridNode node : this.trackedNodes) {
            Object owner = node.getOwner();
            if (!(owner instanceof BlockEntity blockEntity) || !(owner instanceof IChestOrDrive drive)
                    || blockEntity.getLevel() == null || !drive.isPowered()) {
                continue;
            }

            for (int slot = 0; slot < drive.getCellCount(); slot++) {
                StorageCell cell = drive.getOriginalCellInventory(slot);
                if (cell == null) {
                    continue;
                }

                String cellId = t3CellId(blockEntity, slot);
                cells.computeIfAbsent(cellId,
                        ignored -> T3CellInfo.from(cellId, cell, this.config.runtime().maxCandidatesPerRefresh()));
            }
        }
        return cells;
    }

    private static String t3CellId(BlockEntity blockEntity, int slot) {
        ResourceLocation dimensionId = blockEntity.getLevel().dimension().location();
        BlockPos pos = blockEntity.getBlockPos();
        return dimensionId + "@" + pos.getX() + "," + pos.getY() + "," + pos.getZ() + "#" + slot;
    }

    private T2VirusState t2Virus(T2VirusKind kind, ResourceLocation targetId) {
        for (T2VirusState virus : this.t2Viruses) {
            if (virus.kind() == kind && virus.targetId().equals(targetId)) {
                return virus;
            }
        }
        return null;
    }

    private AEKey randomCandidate(List<AEKey> candidates) {
        if (candidates.isEmpty()) {
            return null;
        }
        return candidates.get(this.random.nextInt(candidates.size()));
    }

    private T3VirusState randomT3Virus(List<T3VirusState> candidates) {
        if (candidates.isEmpty()) {
            return null;
        }
        return candidates.get(this.random.nextInt(candidates.size()));
    }

    private long storedAmount(AEKey key) {
        if (key instanceof AEItemKey) {
            return this.riskCache.keyCount(key);
        }
        return 0L;
    }

    private int nextRiskCheckInterval() {
        if (isStimulated()) {
            return InfectionRisk.stimulatedSpreadIntervalTicks();
        }
        int baseInterval = this.config.runtime().riskCheckIntervalTicks();
        long infectedAmount = t1InfectedAmount();
        int virusLevel = highestT1Level();
        int spreadInterval = InfectionRisk.spreadIntervalTicks(baseInterval, infectedAmount, virusLevel, this.config);
        return spreadInterval + this.random.nextInt(Math.max(1, spreadInterval / 4));
    }

    private long spreadGrowthAmount(long infectedAmount, int virusLevel) {
        if (!isStimulated()) {
            return InfectionRisk.spreadGrowthAmount(infectedAmount, virusLevel, this.config);
        }
        return InfectionRisk.stimulatedSpreadGrowthAmount(this.config);
    }

    private ExposureStats activeExposureStats() {
        if (!isStimulated()) {
            return this.riskCache.exposureStats();
        }
        return InfectionRisk.stimulatedExposureStats(this.config);
    }

    private boolean isStimulated() {
        return this.stimulationTicks > 0;
    }

    private void tickStimulation(boolean wasStimulated) {
        if (wasStimulated) {
            this.stimulationTicks--;
        }
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

    private void saveT2Viruses(IGridNode gridNode, CompoundTag savedData) {
        if (this.t2Viruses.isEmpty()) {
            savedData.remove(TAG_T2_VIRUSES);
            return;
        }

        ListTag viruses = new ListTag();
        for (T2VirusState virus : this.t2Viruses) {
            CompoundTag tag = new CompoundTag();
            tag.putString("kind", virus.kind().serializedName());
            tag.putString("targetId", virus.targetId().toString());
            tag.putLong("experience", virus.experience());

            ListTag targets = new ListTag();
            for (Map.Entry<AEKey, Long> entry : virus.blockedAmounts().entrySet()) {
                CompoundTag targetTag = new CompoundTag();
                targetTag.put("target", entry.getKey().toTagGeneric(gridNode.getLevel().registryAccess()));
                targetTag.putLong("blockedAmount", entry.getValue());
                targets.add(targetTag);
            }
            tag.put("targets", targets);
            viruses.add(tag);
        }
        savedData.put(TAG_T2_VIRUSES, viruses);
    }

    private void saveT3Viruses(IGridNode gridNode, CompoundTag savedData) {
        if (this.t3Viruses.isEmpty()) {
            savedData.remove(TAG_T3_VIRUSES);
            return;
        }

        ListTag viruses = new ListTag();
        for (T3VirusState virus : this.t3Viruses) {
            CompoundTag tag = new CompoundTag();
            tag.putString("cellId", virus.cellId());
            tag.putLong("experience", virus.experience());

            ListTag targets = new ListTag();
            for (Map.Entry<AEKey, Long> entry : virus.blockedAmounts().entrySet()) {
                CompoundTag targetTag = new CompoundTag();
                targetTag.put("target", entry.getKey().toTagGeneric(gridNode.getLevel().registryAccess()));
                targetTag.putLong("blockedAmount", entry.getValue());
                targets.add(targetTag);
            }
            tag.put("targets", targets);
            viruses.add(tag);
        }
        savedData.put(TAG_T3_VIRUSES, viruses);
    }

    private void restoreT2Viruses(IGridNode gridNode, CompoundTag savedData) {
        if (savedData == null || !savedData.contains(TAG_T2_VIRUSES, Tag.TAG_LIST)) {
            return;
        }

        ListTag viruses = savedData.getList(TAG_T2_VIRUSES, Tag.TAG_COMPOUND);
        boolean changed = false;
        for (int i = 0; i < viruses.size(); i++) {
            CompoundTag virusTag = viruses.getCompound(i);
            ResourceLocation targetId = ResourceLocation.tryParse(virusTag.getString("targetId"));
            if (targetId == null) {
                continue;
            }

            T2VirusKind kind = T2VirusKind.byName(virusTag.getString("kind"));
            T2VirusState virus = t2Virus(kind, targetId);
            if (virus == null) {
                virus = new T2VirusState(kind, targetId, virusTag.getLong("experience"));
                this.t2Viruses.add(virus);
                changed = true;
            } else if (virus.mergeExperience(virusTag.getLong("experience"))) {
                changed = true;
            }

            ListTag targets = virusTag.getList("targets", Tag.TAG_COMPOUND);
            for (int targetIndex = 0; targetIndex < targets.size(); targetIndex++) {
                CompoundTag targetTag = targets.getCompound(targetIndex);
                AEKey target = AEKey.fromTagGeneric(gridNode.getLevel().registryAccess(),
                        targetTag.getCompound("target"));
                if (target != null && virus.mergeTarget(target, targetTag.getLong("blockedAmount"))) {
                    changed = true;
                }
            }
        }
        if (changed) {
            this.infectionVersion++;
        }
    }

    private void restoreT3Viruses(IGridNode gridNode, CompoundTag savedData) {
        if (savedData == null || !savedData.contains(TAG_T3_VIRUSES, Tag.TAG_LIST)) {
            return;
        }

        ListTag viruses = savedData.getList(TAG_T3_VIRUSES, Tag.TAG_COMPOUND);
        boolean changed = false;
        for (int i = 0; i < viruses.size(); i++) {
            CompoundTag virusTag = viruses.getCompound(i);
            String cellId = virusTag.getString("cellId");
            if (cellId.isBlank()) {
                continue;
            }

            T3VirusState virus = t3Virus(cellId);
            if (virus == null) {
                virus = new T3VirusState(cellId, virusTag.getLong("experience"));
                this.t3Viruses.add(virus);
                changed = true;
            } else if (virus.mergeExperience(virusTag.getLong("experience"))) {
                changed = true;
            }

            ListTag targets = virusTag.getList("targets", Tag.TAG_COMPOUND);
            for (int targetIndex = 0; targetIndex < targets.size(); targetIndex++) {
                CompoundTag targetTag = targets.getCompound(targetIndex);
                AEKey target = AEKey.fromTagGeneric(gridNode.getLevel().registryAccess(),
                        targetTag.getCompound("target"));
                if (target != null && virus.mergeTarget(target, targetTag.getLong("blockedAmount"))) {
                    changed = true;
                }
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

    private long nextLong(long bound) {
        if (bound <= 0L) {
            return 0L;
        }

        long bits;
        long value;
        do {
            bits = this.random.nextLong() >>> 1;
            value = bits % bound;
        } while (bits - value + (bound - 1L) < 0L);
        return value;
    }

    private static final class TickBudget {
        private final long gameTime;
        private int used;

        private TickBudget(long gameTime) {
            this.gameTime = gameTime;
        }
    }

    private static final class TagSeedStats {
        private int seedCount;
        private int seedLevel = 1;
    }

    private record T2SpecializedChoice(ResourceLocation tagId, int seedCount, int susceptibleCount, int seedLevel) {
    }

    private record InfectionGrowth(AEKey target, long amount) {
    }

    private record T3CellInfo(String cellId, Map<AEKey, Long> storedAmounts, long usedAmount) {
        private static T3CellInfo from(String cellId, StorageCell cell, int maxCandidates) {
            Map<AEKey, Long> amounts = new LinkedHashMap<>();
            long fallbackUsedAmount = 0L;
            int count = 0;
            for (var entry : cell.getAvailableStacks()) {
                AEKey key = entry.getKey();
                long amount = entry.getLongValue();
                if (amount > 0) {
                    fallbackUsedAmount = saturatedAdd(fallbackUsedAmount, amount);
                }
                if (amount > 0 && key instanceof AEItemKey itemKey
                        && !(itemKey.getItem() instanceof DataStreamStorageCellItem)) {
                    amounts.put(key, amount);
                    count++;
                    if (count >= maxCandidates) {
                        break;
                    }
                }
            }
            long usedAmount = cellUsedBytes(cell, fallbackUsedAmount);
            return new T3CellInfo(cellId, Map.copyOf(amounts), usedAmount);
        }

        private long storedAmount(AEKey key) {
            return this.storedAmounts.getOrDefault(key, 0L);
        }

        private static long cellUsedBytes(StorageCell cell, long fallbackUsedAmount) {
            try {
                Object usedBytes = cell.getClass().getMethod("getUsedBytes").invoke(cell);
                if (usedBytes instanceof Number number) {
                    return Math.max(0L, number.longValue());
                }
            } catch (ReflectiveOperationException | SecurityException ignored) {
                // Non-basic storage cells do not have to expose byte usage.
            }
            return fallbackUsedAmount;
        }
    }
}
