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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class VirusNetworkService implements IVirusNetworkService, IGridServiceProvider {
    private static final String TAG_T1_VIRUSES = "ae2virus:t1_viruses";
    private static final String TAG_T2_VIRUSES = "ae2virus:t2_viruses";
    private static final Map<ResourceLocation, TickBudget> RISK_CHECK_BUDGETS = new HashMap<>();

    private final IGrid grid;
    private final IStorageService storageService;
    private final InfectionConfig config = InfectionConfig.defaults();
    private final RandomSource random = RandomSource.create();
    private final VirusNetworkRiskCache riskCache = new VirusNetworkRiskCache();
    private final List<T1VirusState> t1Viruses = new ArrayList<>();
    private final List<T2VirusState> t2Viruses = new ArrayList<>();
    private final Set<IGridNode> trackedNodes = new HashSet<>();
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

        if (this.stimulationTicks > 0) {
            this.stimulationTicks--;
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
        runT2RiskCheck();
        this.ticksUntilRiskCheck = nextRiskCheckInterval();
    }

    @Override
    public void addNode(IGridNode gridNode, CompoundTag savedData) {
        this.trackedNodes.add(gridNode);
        this.riskCache.markExposureDirty();
        restoreT1Viruses(gridNode, savedData);
        restoreT2Viruses(gridNode, savedData);
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
        long growth = spreadGrowthAmount(virus.blockedAmount(), virus.level());
        return virus.addBlockedAmount(Math.min(room, growth));
    }

    private static boolean canT1Infect(AEKey key) {
        return VirusInfectionBlacklist.canGenericVirusInfect(key);
    }

    private void runT2RiskCheck() {
        if (this.t1Viruses.isEmpty()) {
            return;
        }

        boolean changed = false;
        if (this.random.nextBoolean()) {
            changed = runT2FusionRiskCheck();
            if (!changed) {
                changed = runT2SpecializedRiskCheck();
            }
        } else {
            changed = runT2SpecializedRiskCheck();
            if (!changed) {
                changed = runT2FusionRiskCheck();
            }
        }

        if (changed) {
            markInfectionChanged();
        }
    }

    private boolean runT2FusionRiskCheck() {
        List<T1VirusState> seeds = t1SeedViruses();
        if (seeds.size() < 2) {
            return false;
        }

        T1VirusState seed = seeds.get(this.random.nextInt(seeds.size()));
        double conversionChance = InfectionRisk.t2FusionConversionChance(seeds.size(), seed.level(), this.config);
        if (this.random.nextDouble() >= conversionChance) {
            return false;
        }

        List<AEKey> fusionTargets = new ArrayList<>();
        for (T1VirusState virus : seeds) {
            fusionTargets.add(virus.target());
        }
        ResourceLocation targetId = T2VirusTargets.fusionTargetId(fusionTargets);
        T2VirusState virus = t2Virus(T2VirusKind.FUSION, targetId);
        if (virus == null) {
            virus = new T2VirusState(T2VirusKind.FUSION, targetId, 1L);
            this.t2Viruses.add(virus);
        }

        AEKey target = randomT2FusionTarget(virus, fusionTargets);
        return target != null && growT2Virus(virus, target);
    }

    private boolean runT2SpecializedRiskCheck() {
        T2SpecializedChoice choice = randomT2SpecializedChoice();
        if (choice == null) {
            return false;
        }

        double conversionChance = InfectionRisk.t2SpecializedConversionChance(choice.seedCount(), choice.seedLevel(),
                this.config);
        if (this.random.nextDouble() >= conversionChance) {
            return false;
        }

        T2VirusState virus = t2Virus(T2VirusKind.SPECIALIZED, choice.tagId());
        if (virus == null) {
            virus = new T2VirusState(T2VirusKind.SPECIALIZED, choice.tagId(), 1L);
            this.t2Viruses.add(virus);
        }

        AEKey target = randomT2SpecializedTarget(virus, choice.tagId());
        return target != null && growT2Virus(virus, target);
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
        return target instanceof AEItemKey
                && !VirusInfectionBlacklist.contains(target)
                && storedAmount(target) > virus.blockedAmount(target);
    }

    private boolean growT2Virus(T2VirusState virus, AEKey target) {
        long storedAmount = storedAmount(target);
        long currentBlocked = virus.blockedAmount(target);
        if (currentBlocked >= storedAmount) {
            return false;
        }

        long room = storedAmount - currentBlocked;
        long growth = spreadGrowthAmount(Math.max(1L, virus.totalBlockedAmount()), virus.level());
        InfectionRoll roll = InfectionRisk.roll(
                InfectionRisk.t2InfectionAttemptChance(Math.max(1, virus.infectedTargetCount() + 1), this.config),
                activeExposureStats(),
                this.random,
                this.config);
        if (roll.result() != InfectionRoll.Result.SUCCESS) {
            return false;
        }

        return virus.addBlockedAmount(target, Math.min(room, growth));
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

    private long storedAmount(AEKey key) {
        if (key instanceof AEItemKey) {
            return this.riskCache.keyCount(key);
        }
        return 0L;
    }

    private int nextRiskCheckInterval() {
        if (isStimulated()) {
            return 1;
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
        return Math.max(1L, (long) Math.floor(this.config.spread().maxSpeedMultiplier()));
    }

    private ExposureStats activeExposureStats() {
        if (!isStimulated()) {
            return this.riskCache.exposureStats();
        }
        return new ExposureStats(0, 0, boostedExposurePressure());
    }

    private double boostedExposurePressure() {
        double maxSuccessChance = this.config.exposure().maxSuccessChance();
        double clamped = Math.max(0.000001, Math.min(0.999999, maxSuccessChance));
        return -Math.log(1.0 - clamped) * this.config.exposure().scale();
    }

    private boolean isStimulated() {
        return this.stimulationTicks > 0;
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

    private static final class TagSeedStats {
        private int seedCount;
        private int seedLevel = 1;
    }

    private record T2SpecializedChoice(ResourceLocation tagId, int seedCount, int susceptibleCount, int seedLevel) {
    }
}
