package com.java.beipuo.ae2virus.infection;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public final class VirusNetworkRiskCache {
    private final Object2LongOpenHashMap<Item> itemCounts = new Object2LongOpenHashMap<>();
    private final Object2LongOpenHashMap<ResourceLocation> tagItemCounts = new Object2LongOpenHashMap<>();
    private final Object2ObjectOpenHashMap<ResourceLocation, List<AEKey>> tagCandidates = new Object2ObjectOpenHashMap<>();
    private final Object2IntOpenHashMap<VirusClass> virusCounts = new Object2IntOpenHashMap<>();
    private final EnumMap<VirusClass, List<AEKey>> candidates = new EnumMap<>(VirusClass.class);
    private final Long2LongOpenHashMap diskUsedBytes = new Long2LongOpenHashMap();
    private final Long2ObjectOpenHashMap<List<AEKey>> diskCandidates = new Long2ObjectOpenHashMap<>();
    private final Long2IntOpenHashMap driveInfectedDiskCounts = new Long2IntOpenHashMap();
    private final Long2ObjectOpenHashMap<List<AEKey>> driveCandidates = new Long2ObjectOpenHashMap<>();

    private long networkDiskUsedBytes;
    private long totalBytes;
    private long blacklistedItemCount;
    private ExposureStats exposureStats = ExposureStats.NONE;
    private boolean storageDirty = true;
    private boolean exposureDirty = true;
    private boolean virusDirty = true;
    private boolean candidatesDirty = true;

    public VirusNetworkRiskCache() {
        for (VirusClass virusClass : VirusClass.values()) {
            this.candidates.put(virusClass, List.of());
        }
    }

    public void rebuildStorage(KeyCounter inventory, int maxCandidates) {
        this.itemCounts.clear();
        this.tagItemCounts.clear();
        this.tagCandidates.clear();

        List<AEKey> itemCandidates = new ArrayList<>();
        List<AEKey> polymorphicCandidates = new ArrayList<>();
        long totalStoredAmount = 0L;
        long blacklistedItemCount = 0L;
        for (var entry : inventory) {
            AEKey key = entry.getKey();
            long amount = entry.getLongValue();
            if (amount <= 0) {
                continue;
            }

            totalStoredAmount = saturatedAdd(totalStoredAmount, amount);
            if (key instanceof AEItemKey itemKey) {
                Item item = itemKey.getItem();
                this.itemCounts.addTo(item, amount);
                item.builtInRegistryHolder().tags().forEach(tag -> {
                    ResourceLocation tagId = tag.location();
                    this.tagItemCounts.addTo(tagId, amount);
                    this.tagCandidates.computeIfAbsent(tagId, ignored -> new ArrayList<>()).add(key);
                });
                if (itemCandidates.size() < maxCandidates) {
                    itemCandidates.add(key);
                }
                if (item == Items.NETHER_STAR) {
                    blacklistedItemCount = saturatedAdd(blacklistedItemCount, amount);
                    if (polymorphicCandidates.size() < maxCandidates) {
                        polymorphicCandidates.add(key);
                    }
                }
            }
        }

        this.networkDiskUsedBytes = totalStoredAmount;
        this.totalBytes = totalStoredAmount;
        this.blacklistedItemCount = blacklistedItemCount;
        this.candidates.put(VirusClass.TARGETED, List.copyOf(itemCandidates));
        this.candidates.put(VirusClass.BROAD_SPECTRUM, broadSpectrumCandidates(maxCandidates));
        this.candidates.put(VirusClass.SYSTEMIC, List.copyOf(itemCandidates));
        this.candidates.put(VirusClass.POLYMORPHIC, List.copyOf(polymorphicCandidates));
        this.candidatesDirty = false;
        this.storageDirty = false;
    }

    public void clearStorageContexts() {
        this.diskUsedBytes.clear();
        this.diskCandidates.clear();
        this.driveInfectedDiskCounts.clear();
        this.driveCandidates.clear();
    }

    public void addDiskContext(long diskId, long usedBytes, List<AEKey> candidates) {
        if (usedBytes <= 0) {
            return;
        }
        this.diskUsedBytes.put(diskId, usedBytes);
        this.diskCandidates.put(diskId, List.copyOf(candidates));
    }

    public void addDriveContext(long driveId, List<AEKey> candidates) {
        if (!candidates.isEmpty()) {
            this.driveCandidates.put(driveId, List.copyOf(candidates));
        }
    }

    public void setExposureStats(ExposureStats exposureStats) {
        this.exposureStats = exposureStats;
        this.exposureDirty = false;
    }

    public void markExposureDirty() {
        this.exposureDirty = true;
    }

    public void addVirus(VirusClass virusClass) {
        this.virusCounts.addTo(virusClass, 1);
        this.virusDirty = false;
        this.candidatesDirty = true;
    }

    public boolean removeVirus(VirusClass virusClass) {
        int count = this.virusCounts.getInt(virusClass);
        if (count <= 0) {
            return false;
        }

        if (count == 1) {
            this.virusCounts.removeInt(virusClass);
        } else {
            this.virusCounts.put(virusClass, count - 1);
        }
        this.virusDirty = false;
        this.candidatesDirty = true;
        return true;
    }

    public boolean clearVirus(VirusClass virusClass) {
        if (this.virusCounts.removeInt(virusClass) <= 0) {
            return false;
        }
        this.virusDirty = false;
        this.candidatesDirty = true;
        return true;
    }

    public boolean restoreVirusCount(VirusClass virusClass, int count) {
        if (count <= 0 || this.virusCounts.getInt(virusClass) >= count) {
            return false;
        }

        this.virusCounts.put(virusClass, count);
        this.virusDirty = false;
        this.candidatesDirty = true;
        return true;
    }

    public long itemCount(Item item) {
        return this.itemCounts.getLong(item);
    }

    public long tagItemCount(ResourceLocation tagId) {
        return this.tagItemCounts.getLong(tagId);
    }

    public ResourceLocation strongestTag() {
        ResourceLocation strongest = null;
        long strongestAmount = 0L;
        for (var entry : this.tagItemCounts.object2LongEntrySet()) {
            if (entry.getLongValue() > strongestAmount) {
                strongest = entry.getKey();
                strongestAmount = entry.getLongValue();
            }
        }
        return strongest;
    }

    public ResourceLocation strongestCompleteTag() {
        ResourceLocation strongest = null;
        long strongestAmount = 0L;
        for (var entry : this.tagItemCounts.object2LongEntrySet()) {
            if (entry.getLongValue() > strongestAmount && hasEveryItemInTag(entry.getKey())) {
                strongest = entry.getKey();
                strongestAmount = entry.getLongValue();
            }
        }
        return strongest;
    }

    public ResourceLocation strongestTagInFamily(ResourceLocation rootTagId) {
        ResourceLocation strongest = null;
        long strongestAmount = 0L;
        for (var entry : this.tagItemCounts.object2LongEntrySet()) {
            if (isTagInFamily(entry.getKey(), rootTagId) && entry.getLongValue() > strongestAmount) {
                strongest = entry.getKey();
                strongestAmount = entry.getLongValue();
            }
        }
        return strongest;
    }

    public List<AEKey> candidatesForTag(ResourceLocation tagId) {
        List<AEKey> keys = this.tagCandidates.get(tagId);
        return keys == null ? List.of() : List.copyOf(keys);
    }

    public List<AEKey> candidatesForTagFamily(ResourceLocation rootTagId) {
        List<AEKey> result = new ArrayList<>();
        for (Map.Entry<ResourceLocation, List<AEKey>> entry : this.tagCandidates.entrySet()) {
            if (!isTagInFamily(entry.getKey(), rootTagId)) {
                continue;
            }
            for (AEKey key : entry.getValue()) {
                if (!result.contains(key)) {
                    result.add(key);
                }
            }
        }
        return List.copyOf(result);
    }

    public boolean hasEveryItemInTag(ResourceLocation tagId) {
        var tagKey = TagKey.create(Registries.ITEM, tagId);
        var tag = BuiltInRegistries.ITEM.getTag(tagKey);
        if (tag.isEmpty()) {
            return false;
        }

        boolean hasAnyItem = false;
        for (var holder : tag.get()) {
            Item item = holder.value();
            if (item == Items.AIR) {
                continue;
            }
            hasAnyItem = true;
            if (this.itemCounts.getLong(item) <= 0) {
                return false;
            }
        }
        return hasAnyItem;
    }

    public long diskUsedBytes(long diskId) {
        return diskId == InfectionTarget.BroadSpectrumTarget.NETWORK_DISK_ID
                ? this.networkDiskUsedBytes
                : this.diskUsedBytes.get(diskId);
    }

    public int infectedDiskCount(long driveId) {
        return this.driveInfectedDiskCounts.get(driveId);
    }

    public void setDriveInfectedDiskCount(long driveId, int infectedDiskCount) {
        if (infectedDiskCount <= 0) {
            this.driveInfectedDiskCounts.remove(driveId);
        } else {
            this.driveInfectedDiskCounts.put(driveId, infectedDiskCount);
        }
    }

    public long strongestDiskId() {
        long strongestId = InfectionTarget.BroadSpectrumTarget.NETWORK_DISK_ID;
        long strongestAmount = 0L;
        for (var entry : this.diskUsedBytes.long2LongEntrySet()) {
            if (entry.getLongValue() > strongestAmount) {
                strongestId = entry.getLongKey();
                strongestAmount = entry.getLongValue();
            }
        }
        return strongestId;
    }

    public boolean hasDiskContexts() {
        return !this.diskUsedBytes.isEmpty();
    }

    public long strongestDriveId() {
        long strongestId = InfectionTarget.BroadSpectrumTarget.NETWORK_DRIVE_ID;
        int strongestCount = 0;
        for (var entry : this.driveInfectedDiskCounts.long2IntEntrySet()) {
            if (entry.getIntValue() > strongestCount) {
                strongestId = entry.getLongKey();
                strongestCount = entry.getIntValue();
            }
        }
        return strongestId;
    }

    public List<AEKey> candidatesForDisk(long diskId) {
        if (diskId == InfectionTarget.BroadSpectrumTarget.NETWORK_DISK_ID) {
            return candidates(VirusClass.BROAD_SPECTRUM);
        }
        return this.diskCandidates.getOrDefault(diskId, List.of());
    }

    public List<AEKey> candidatesForDrive(long driveId) {
        if (driveId == InfectionTarget.BroadSpectrumTarget.NETWORK_DRIVE_ID) {
            return candidates(VirusClass.BROAD_SPECTRUM);
        }
        return this.driveCandidates.getOrDefault(driveId, List.of());
    }

    public long totalBytes() {
        return this.totalBytes;
    }

    public long blacklistedItemCount() {
        return this.blacklistedItemCount;
    }

    public int virusCount(VirusClass virusClass) {
        return this.virusCounts.getInt(virusClass);
    }

    public ExposureStats exposureStats() {
        return this.exposureStats;
    }

    public List<AEKey> candidates(VirusClass virusClass) {
        return this.candidates.getOrDefault(virusClass, List.of());
    }

    public boolean storageDirty() {
        return this.storageDirty;
    }

    public boolean exposureDirty() {
        return this.exposureDirty;
    }

    public boolean virusDirty() {
        return this.virusDirty;
    }

    public boolean candidatesDirty() {
        return this.candidatesDirty;
    }

    private static long saturatedAdd(long left, long right) {
        long result = left + right;
        return result < 0 ? Long.MAX_VALUE : result;
    }

    private List<AEKey> broadSpectrumCandidates(int maxCandidates) {
        List<AEKey> result = new ArrayList<>();
        for (Map.Entry<ResourceLocation, List<AEKey>> entry : this.tagCandidates.entrySet()) {
            for (AEKey key : entry.getValue()) {
                if (result.size() >= maxCandidates) {
                    return List.copyOf(result);
                }
                if (!result.contains(key)) {
                    result.add(key);
                }
            }
        }
        return List.copyOf(result);
    }

    private static boolean isTagInFamily(ResourceLocation tagId, ResourceLocation rootTagId) {
        return tagId.equals(rootTagId)
                || tagId.getNamespace().equals(rootTagId.getNamespace())
                        && tagId.getPath().startsWith(rootTagId.getPath() + "/");
    }
}
