package com.java.beipuo.ae2virus.infection;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridServiceProvider;
import appeng.api.implementations.parts.ICablePart;
import appeng.api.networking.storage.IStorageService;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.util.AECableType;
import appeng.api.storage.MEStorage;
import appeng.blockentity.storage.DriveBlockEntity;
import appeng.parts.AEBasePart;
import java.util.ArrayList;
import java.util.EnumMap;
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
import net.neoforged.neoforge.common.Tags;

public final class VirusNetworkService implements IVirusNetworkService, IGridServiceProvider {
    private static final String TAG_INFECTED_BY_CLASS = "ae2virus:infected_by_class";
    private static final String TAG_VIRUS_COUNTS = "ae2virus:virus_counts";
    private static final String TAG_BROAD_SPECTRUM_TARGETS = "ae2virus:broad_spectrum_targets";
    private static final ResourceLocation INGOTS_TAG_ID = Tags.Items.INGOTS.location();
    private static final long CONTEXT_HASH_SEED = 0xcbf29ce484222325L;
    private static final long CONTEXT_HASH_PRIME = 0x100000001b3L;
    private static final Map<ResourceLocation, TickBudget> RISK_CHECK_BUDGETS = new HashMap<>();

    private final IGrid grid;
    private final IStorageService storageService;
    private final InfectionConfig config = InfectionConfig.defaults();
    private final RandomSource random = RandomSource.create();
    private final VirusNetworkRiskCache riskCache = new VirusNetworkRiskCache();
    private final EnumMap<VirusClass, Set<AEKey>> infectedKeysByClass = new EnumMap<>(VirusClass.class);
    private final Set<InfectionTarget.BroadSpectrumTarget> activeBroadSpectrumTargets = new HashSet<>();
    private final Set<IGridNode> trackedNodes = new HashSet<>();
    private VirusNetworkStats stats = VirusNetworkStats.EMPTY;
    private int ticksUntilRiskCheck;
    private int infectionVersion;

    public VirusNetworkService(IGrid grid, IStorageService storageService) {
        this.grid = grid;
        this.storageService = storageService;
        for (VirusClass virusClass : VirusClass.values()) {
            this.infectedKeysByClass.put(virusClass, new HashSet<>());
        }
        VirusNetworkStorageGuards.register(storageService.getInventory(), this);
        this.ticksUntilRiskCheck = 20 + this.random.nextInt(Math.max(1, this.config.riskCheckIntervalTicks()));
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

        this.ticksUntilRiskCheck = this.config.riskCheckIntervalTicks()
                + this.random.nextInt(Math.max(1, this.config.riskCheckIntervalTicks() / 4));
        refreshStats();
        runRiskCheck();
    }

    @Override
    public void addNode(IGridNode gridNode, CompoundTag savedData) {
        this.trackedNodes.add(gridNode);
        this.riskCache.markExposureDirty();
        restoreInfectedKeys(gridNode, savedData);
        restoreVirusCounts(savedData);
        restoreBroadSpectrumTargets(savedData);
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
    public int debugAddVirusAndInfect(VirusClass virusClass) {
        refreshStats();
        int visibleBefore = currentVisibleInfectedKeys().size();
        this.riskCache.addVirus(virusClass);

        Set<AEKey> infectedKeys = this.infectedKeysByClass.get(virusClass);
        if (virusClass == VirusClass.BROAD_SPECTRUM) {
            activateDebugBroadSpectrumTarget();
        }
        for (AEKey key : debugInfectionTargets(virusClass)) {
            infectedKeys.add(key);
        }
        markInfectionChanged();
        return Math.max(0, currentVisibleInfectedKeys().size() - visibleBefore);
    }

    @Override
    public int debugRemoveVirusAndClear(VirusClass virusClass) {
        refreshStats();
        int visibleBefore = currentVisibleInfectedKeys().size();
        Set<AEKey> infectedKeys = this.infectedKeysByClass.get(virusClass);
        int before = infectedKeys.size();
        infectedKeys.clear();
        boolean broadTargetsChanged = virusClass == VirusClass.BROAD_SPECTRUM
                && !this.activeBroadSpectrumTargets.isEmpty();
        if (broadTargetsChanged) {
            this.activeBroadSpectrumTargets.clear();
        }
        if (infectedKeys.size() != before || broadTargetsChanged || this.riskCache.clearVirus(virusClass)) {
            markInfectionChanged();
        }
        return Math.max(0, visibleBefore - currentVisibleInfectedKeys().size());
    }

    @Override
    public void saveNodeData(IGridNode gridNode, CompoundTag savedData) {
        CompoundTag infectedByClass = new CompoundTag();
        for (VirusClass virusClass : VirusClass.values()) {
            Set<AEKey> infectedKeys = this.infectedKeysByClass.get(virusClass);
            if (infectedKeys.isEmpty()) {
                continue;
            }

            ListTag keys = new ListTag();
            for (AEKey key : infectedKeys) {
                keys.add(key.toTagGeneric(gridNode.getLevel().registryAccess()));
            }
            infectedByClass.put(virusClass.name(), keys);
        }
        if (!infectedByClass.isEmpty()) {
            savedData.put(TAG_INFECTED_BY_CLASS, infectedByClass);
        }

        CompoundTag virusCounts = new CompoundTag();
        for (VirusClass virusClass : VirusClass.values()) {
            int count = this.riskCache.virusCount(virusClass);
            if (count > 0) {
                virusCounts.putInt(virusClass.name(), count);
            }
        }
        if (!virusCounts.isEmpty()) {
            savedData.put(TAG_VIRUS_COUNTS, virusCounts);
        }

        if (!this.activeBroadSpectrumTargets.isEmpty()) {
            ListTag broadTargets = new ListTag();
            for (InfectionTarget.BroadSpectrumTarget target : this.activeBroadSpectrumTargets) {
                CompoundTag targetTag = new CompoundTag();
                targetTag.putString("variant", target.broadVariant().name());
                if (target.tagId() != null) {
                    targetTag.putString("tag", target.tagId().toString());
                }
                targetTag.putLong("disk", target.diskId());
                targetTag.putLong("drive", target.driveId());
                broadTargets.add(targetTag);
            }
            savedData.put(TAG_BROAD_SPECTRUM_TARGETS, broadTargets);
        }
    }

    @Override
    public boolean isInfected(AEKey key) {
        if (isDirectlyInfected(key)) {
            return true;
        }
        if (isActiveBroadSpectrumInfection(key)) {
            return true;
        }
        if (isVirusClassActive(VirusClass.SYSTEMIC) && key instanceof AEItemKey) {
            return true;
        }
        return isVirusClassActive(VirusClass.POLYMORPHIC) && isNetherStarKey(key);
    }

    private boolean isDirectlyInfected(AEKey key) {
        for (Set<AEKey> infectedKeys : this.infectedKeysByClass.values()) {
            if (infectedKeys.contains(key)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Set<AEKey> getInfectedKeys() {
        return Set.copyOf(currentVisibleInfectedKeys());
    }

    @Override
    public int getInfectionVersion() {
        return this.infectionVersion;
    }

    private void refreshStats() {
        this.riskCache.rebuildStorage(this.storageService.getCachedInventory(), this.config.maxCandidatesPerRefresh());
        rebuildStorageContexts();
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
            if (key instanceof AEItemKey && itemCandidates.size() < this.config.maxCandidatesPerRefresh()) {
                itemCandidates.add(key);
            }
        }

        this.stats = new VirusNetworkStats(storedKeyCount, totalStoredAmount, List.copyOf(itemCandidates));
    }

    private void runRiskCheck() {
        runTargetedRiskCheck();
        runBroadSpectrumRiskCheck();
        runSystemicRiskCheck();
        runPolymorphicRiskCheck();
    }

    private void runTargetedRiskCheck() {
        List<AEKey> targetedCandidates = this.riskCache.candidates(VirusClass.TARGETED);
        if (targetedCandidates.isEmpty()) {
            return;
        }

        AEKey targetKey = targetedCandidates.get(this.random.nextInt(targetedCandidates.size()));
        if (!(targetKey instanceof AEItemKey itemKey)) {
            return;
        }

        InfectionRoll roll = InfectionRisk.roll(
                VirusClass.TARGETED,
                new InfectionTarget.ItemTarget(itemKey.getItem()),
                this.riskCache,
                this.riskCache.exposureStats(),
                this.random,
                this.config);
        if (roll.result() == InfectionRoll.Result.SUCCESS) {
            if (infect(VirusClass.TARGETED, targetKey)) {
                this.riskCache.addVirus(VirusClass.TARGETED);
                markInfectionChanged();
            }
        }
    }

    private void runBroadSpectrumRiskCheck() {
        InfectionTarget.BroadSpectrumTarget target = chooseBroadSpectrumTarget();
        if (target == null) {
            return;
        }

        InfectionRoll roll = InfectionRisk.roll(
                VirusClass.BROAD_SPECTRUM,
                target,
                this.riskCache,
                this.riskCache.exposureStats(),
                this.random,
                this.config);
        if (roll.result() != InfectionRoll.Result.SUCCESS) {
            return;
        }

        boolean changed = false;
        this.activeBroadSpectrumTargets.add(target);
        List<AEKey> affectedKeys = broadSpectrumCandidatesForTarget(target);
        for (AEKey key : affectedKeys) {
            changed |= infect(VirusClass.BROAD_SPECTRUM, key);
        }
        this.riskCache.addVirus(VirusClass.BROAD_SPECTRUM);
        markInfectionChanged();
    }

    private void runSystemicRiskCheck() {
        List<AEKey> systemicCandidates = this.riskCache.candidates(VirusClass.SYSTEMIC);
        if (systemicCandidates.isEmpty()) {
            return;
        }

        InfectionRoll roll = InfectionRisk.roll(
                VirusClass.SYSTEMIC,
                InfectionTarget.None.INSTANCE,
                this.riskCache,
                this.riskCache.exposureStats(),
                this.random,
                this.config);
        if (roll.result() != InfectionRoll.Result.SUCCESS) {
            return;
        }

        boolean changed = false;
        for (AEKey key : systemicCandidates) {
            changed |= infect(VirusClass.SYSTEMIC, key);
        }
        if (changed) {
            this.riskCache.addVirus(VirusClass.SYSTEMIC);
            markInfectionChanged();
        }
    }

    private void runPolymorphicRiskCheck() {
        List<AEKey> polymorphicCandidates = this.riskCache.candidates(VirusClass.POLYMORPHIC);
        if (polymorphicCandidates.isEmpty()) {
            return;
        }

        InfectionRoll roll = InfectionRisk.roll(
                VirusClass.POLYMORPHIC,
                InfectionTarget.None.INSTANCE,
                this.riskCache,
                this.riskCache.exposureStats(),
                this.random,
                this.config);
        if (roll.result() != InfectionRoll.Result.SUCCESS) {
            return;
        }

        boolean changed = false;
        for (AEKey key : polymorphicCandidates) {
            changed |= infect(VirusClass.POLYMORPHIC, key);
        }
        if (changed) {
            this.riskCache.addVirus(VirusClass.POLYMORPHIC);
            markInfectionChanged();
        }
    }

    private List<AEKey> debugInfectionTargets(VirusClass virusClass) {
        return switch (virusClass) {
            case TARGETED -> randomSingleTarget(this.riskCache.candidates(VirusClass.TARGETED));
            case BROAD_SPECTRUM -> broadSpectrumCandidatesForTarget(debugBroadSpectrumTarget());
            case SYSTEMIC -> this.riskCache.candidates(VirusClass.SYSTEMIC);
            case POLYMORPHIC -> netherStarTargets();
        };
    }

    private List<AEKey> randomSingleTarget(List<AEKey> candidates) {
        if (candidates.isEmpty()) {
            return List.of();
        }
        return List.of(candidates.get(this.random.nextInt(candidates.size())));
    }

    private List<AEKey> netherStarTargets() {
        AEKey key = AEItemKey.of(Items.NETHER_STAR);
        if (this.riskCache.itemCount(Items.NETHER_STAR) <= 0) {
            return List.of();
        }
        return List.of(key);
    }

    private InfectionTarget.BroadSpectrumTarget chooseBroadSpectrumTarget() {
        List<InfectionTarget.BroadSpectrumTarget> candidates = new ArrayList<>();

        ResourceLocation tagId = strongestCompleteTag();
        if (tagId != null) {
            candidates.add(InfectionTarget.BroadSpectrumTarget.tag(tagId));
        }
        long diskId = this.riskCache.strongestDiskId();
        if (this.riskCache.diskUsedBytes(diskId) > 0) {
            candidates.add(InfectionTarget.BroadSpectrumTarget.disk(diskId));
        }
        long driveId = this.riskCache.strongestDriveId();
        if (this.riskCache.infectedDiskCount(driveId) > 0) {
            candidates.add(InfectionTarget.BroadSpectrumTarget.drive(driveId));
        }

        candidates.removeIf(this.activeBroadSpectrumTargets::contains);
        if (candidates.isEmpty()) {
            return null;
        }
        return candidates.get(this.random.nextInt(candidates.size()));
    }

    private ResourceLocation strongestCompleteTag() {
        return this.riskCache.strongestCompleteTag();
    }

    private InfectionTarget.BroadSpectrumTarget debugBroadSpectrumTarget() {
        ResourceLocation tagId = this.riskCache.hasEveryItemInTag(INGOTS_TAG_ID)
                ? INGOTS_TAG_ID
                : this.riskCache.strongestTagInFamily(INGOTS_TAG_ID);
        if (tagId != null) {
            return InfectionTarget.BroadSpectrumTarget.tag(tagId);
        }
        return InfectionTarget.BroadSpectrumTarget.disk(InfectionTarget.BroadSpectrumTarget.NETWORK_DISK_ID);
    }

    private void activateDebugBroadSpectrumTarget() {
        this.activeBroadSpectrumTargets.add(debugBroadSpectrumTarget());
    }

    private List<AEKey> broadSpectrumCandidatesForTarget(InfectionTarget.BroadSpectrumTarget target) {
        return switch (target.broadVariant()) {
            case TAG -> target.tagId() == null ? List.of() : this.riskCache.candidatesForTag(target.tagId());
            case DISK -> this.riskCache.candidatesForDisk(target.diskId());
            case DRIVE -> this.riskCache.candidatesForDrive(target.driveId());
        };
    }

    private boolean isVirusClassActive(VirusClass virusClass) {
        return this.riskCache.virusCount(virusClass) > 0 || !this.infectedKeysByClass.get(virusClass).isEmpty();
    }

    private void restoreInfectedKeys(IGridNode gridNode, CompoundTag savedData) {
        if (savedData == null || !savedData.contains(TAG_INFECTED_BY_CLASS, Tag.TAG_COMPOUND)) {
            return;
        }

        restoreInfectedKeysByClass(gridNode, savedData.getCompound(TAG_INFECTED_BY_CLASS));
    }

    private void restoreInfectedKeysByClass(IGridNode gridNode, CompoundTag infectedByClass) {
        boolean changed = false;
        for (VirusClass virusClass : VirusClass.values()) {
            if (!infectedByClass.contains(virusClass.name(), Tag.TAG_LIST)) {
                continue;
            }

            ListTag keys = infectedByClass.getList(virusClass.name(), Tag.TAG_COMPOUND);
            for (int i = 0; i < keys.size(); i++) {
                AEKey key = AEKey.fromTagGeneric(gridNode.getLevel().registryAccess(), keys.getCompound(i));
                if (key != null) {
                    changed |= infect(virusClass, key);
                }
            }
        }
        if (changed) {
            this.infectionVersion++;
        }
    }

    private void restoreVirusCounts(CompoundTag savedData) {
        if (savedData == null || !savedData.contains(TAG_VIRUS_COUNTS, Tag.TAG_COMPOUND)) {
            return;
        }

        CompoundTag virusCounts = savedData.getCompound(TAG_VIRUS_COUNTS);
        boolean changed = false;
        for (VirusClass virusClass : VirusClass.values()) {
            if (virusCounts.contains(virusClass.name(), Tag.TAG_INT)) {
                changed |= this.riskCache.restoreVirusCount(virusClass, virusCounts.getInt(virusClass.name()));
            }
        }
        if (changed) {
            this.infectionVersion++;
        }
    }

    private void restoreBroadSpectrumTargets(CompoundTag savedData) {
        if (savedData == null || !savedData.contains(TAG_BROAD_SPECTRUM_TARGETS, Tag.TAG_LIST)) {
            return;
        }

        ListTag targets = savedData.getList(TAG_BROAD_SPECTRUM_TARGETS, Tag.TAG_COMPOUND);
        boolean changed = false;
        for (int i = 0; i < targets.size(); i++) {
            CompoundTag targetTag = targets.getCompound(i);
            BroadSpectrumVariant variant;
            try {
                variant = BroadSpectrumVariant.valueOf(targetTag.getString("variant"));
            } catch (IllegalArgumentException ignored) {
                continue;
            }

            InfectionTarget.BroadSpectrumTarget target = switch (variant) {
                case TAG -> targetTag.contains("tag", Tag.TAG_STRING)
                        ? InfectionTarget.BroadSpectrumTarget.tag(ResourceLocation.parse(targetTag.getString("tag")))
                        : null;
                case DISK -> InfectionTarget.BroadSpectrumTarget.disk(targetTag.getLong("disk"));
                case DRIVE -> InfectionTarget.BroadSpectrumTarget.drive(targetTag.getLong("drive"));
            };
            if (target != null) {
                changed |= this.activeBroadSpectrumTargets.add(target);
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

    private boolean infect(VirusClass virusClass, AEKey key) {
        return this.infectedKeysByClass.get(virusClass).add(key);
    }

    private Set<AEKey> currentVisibleInfectedKeys() {
        Set<AEKey> result = new HashSet<>();
        for (Set<AEKey> infectedKeys : this.infectedKeysByClass.values()) {
            result.addAll(infectedKeys);
        }
        if (isVirusClassActive(VirusClass.BROAD_SPECTRUM)
                || isVirusClassActive(VirusClass.SYSTEMIC)
                || isVirusClassActive(VirusClass.POLYMORPHIC)) {
            for (var entry : this.storageService.getCachedInventory()) {
                AEKey key = entry.getKey();
                if (entry.getLongValue() > 0 && isInfected(key)) {
                    result.add(key);
                }
            }
        }
        return result;
    }

    private boolean isActiveBroadSpectrumInfection(AEKey key) {
        if (!isVirusClassActive(VirusClass.BROAD_SPECTRUM)) {
            return false;
        }

        for (InfectionTarget.BroadSpectrumTarget target : this.activeBroadSpectrumTargets) {
            if (matchesBroadSpectrumTarget(key, target)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesBroadSpectrumTarget(AEKey key, InfectionTarget.BroadSpectrumTarget target) {
        return switch (target.broadVariant()) {
            case TAG -> target.tagId() != null && isKeyInTag(key, target.tagId());
            case DISK -> this.riskCache.candidatesForDisk(target.diskId()).contains(key);
            case DRIVE -> this.riskCache.candidatesForDrive(target.driveId()).contains(key);
        };
    }

    private static boolean isKeyInTag(AEKey key, ResourceLocation tagId) {
        if (!(key instanceof AEItemKey itemKey)) {
            return false;
        }

        return itemKey.getItem().builtInRegistryHolder().tags()
                .anyMatch(tag -> tag.location().equals(tagId));
    }

    private static boolean isNetherStarKey(AEKey key) {
        return key instanceof AEItemKey itemKey && itemKey.getItem() == Items.NETHER_STAR;
    }

    private static boolean isTagInFamily(ResourceLocation tagId, ResourceLocation rootTagId) {
        return tagId.equals(rootTagId)
                || tagId.getNamespace().equals(rootTagId.getNamespace())
                        && tagId.getPath().startsWith(rootTagId.getPath() + "/");
    }

    private void rebuildStorageContexts() {
        this.riskCache.clearStorageContexts();
        for (IGridNode node : this.trackedNodes) {
            Object owner = node.getOwner();
            if (owner instanceof DriveBlockEntity drive) {
                rebuildDriveContext(drive);
            }
        }
    }

    private void rebuildDriveContext(DriveBlockEntity drive) {
        Level level = drive.getLevel();
        if (level == null) {
            return;
        }

        long driveId = contextId(level.dimension().location(), drive.getBlockPos(), -1);
        List<AEKey> driveCandidates = new ArrayList<>();
        int infectedDiskCount = 0;
        for (int slot = 0; slot < drive.getCellCount(); slot++) {
            MEStorage cellInventory = drive.getCellInventory(slot);
            if (cellInventory == null) {
                continue;
            }

            KeyCounter cellStacks = new KeyCounter();
            cellInventory.getAvailableStacks(cellStacks);
            long diskUsedBytes = 0L;
            List<AEKey> diskCandidates = new ArrayList<>();
            boolean diskInfected = false;
            for (var entry : cellStacks) {
                AEKey key = entry.getKey();
                long amount = entry.getLongValue();
                if (amount <= 0) {
                    continue;
                }

                diskUsedBytes = saturatedAdd(diskUsedBytes, amount);
                if (key instanceof AEItemKey && diskCandidates.size() < this.config.maxCandidatesPerRefresh()) {
                    diskCandidates.add(key);
                    if (!driveCandidates.contains(key) && driveCandidates.size() < this.config.maxCandidatesPerRefresh()) {
                        driveCandidates.add(key);
                    }
                }
                diskInfected |= isDirectlyInfected(key) || isDiskBroadSpectrumInfected(key);
            }

            if (diskUsedBytes > 0) {
                long diskId = contextId(level.dimension().location(), drive.getBlockPos(), slot);
                this.riskCache.addDiskContext(diskId, diskUsedBytes, diskCandidates);
                if (diskInfected || isActiveDiskTarget(diskId)) {
                    infectedDiskCount++;
                }
            }
        }

        this.riskCache.addDriveContext(driveId, driveCandidates);
        this.riskCache.setDriveInfectedDiskCount(driveId, infectedDiskCount);
    }

    private boolean isDiskBroadSpectrumInfected(AEKey key) {
        for (InfectionTarget.BroadSpectrumTarget target : this.activeBroadSpectrumTargets) {
            if (target.broadVariant() == BroadSpectrumVariant.DISK
                    && this.riskCache.candidatesForDisk(target.diskId()).contains(key)) {
                return true;
            }
        }
        return false;
    }

    private boolean isActiveDiskTarget(long diskId) {
        return this.activeBroadSpectrumTargets.stream()
                .anyMatch(target -> target.broadVariant() == BroadSpectrumVariant.DISK && target.diskId() == diskId);
    }

    private static long contextId(ResourceLocation dimension, BlockPos pos, int slot) {
        long hash = CONTEXT_HASH_SEED;
        hash = hashString(hash, dimension.toString());
        hash = hashLong(hash, pos.asLong());
        hash = hashLong(hash, slot);
        return hash == InfectionTarget.BroadSpectrumTarget.NETWORK_DISK_ID
                || hash == InfectionTarget.BroadSpectrumTarget.NETWORK_DRIVE_ID ? 1L : hash;
    }

    private static long hashString(long hash, String value) {
        long result = hash;
        for (int i = 0; i < value.length(); i++) {
            result ^= value.charAt(i);
            result *= CONTEXT_HASH_PRIME;
        }
        return result;
    }

    private static long hashLong(long hash, long value) {
        long result = hash;
        for (int shift = 0; shift < Long.SIZE; shift += Byte.SIZE) {
            result ^= value >>> shift & 0xffL;
            result *= CONTEXT_HASH_PRIME;
        }
        return result;
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
        return countAirFaces(level, pos) * this.config.cableFaceWeight() * 2.0;
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
        if (budget.used >= this.config.maxRiskChecksPerTick()) {
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
