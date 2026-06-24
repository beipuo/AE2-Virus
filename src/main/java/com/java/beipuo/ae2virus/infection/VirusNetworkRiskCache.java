package com.java.beipuo.ae2virus.infection;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class VirusNetworkRiskCache {
    private final Map<AEKey, Long> keyCounts = new HashMap<>();
    private List<AEKey> itemCandidates = List.of();
    private ExposureStats exposureStats = ExposureStats.NONE;
    private boolean exposureDirty = true;

    public void rebuildStorage(KeyCounter inventory, int maxCandidates) {
        this.keyCounts.clear();
        List<AEKey> candidates = new ArrayList<>();

        for (var entry : inventory) {
            AEKey key = entry.getKey();
            long amount = entry.getLongValue();
            if (amount <= 0 || !(key instanceof AEItemKey itemKey)) {
                continue;
            }

            this.keyCounts.merge(key, amount, VirusNetworkRiskCache::saturatedAdd);
            if (candidates.size() < maxCandidates) {
                candidates.add(key);
            }
        }

        this.itemCandidates = List.copyOf(candidates);
    }

    public long keyCount(AEKey key) {
        return this.keyCounts.getOrDefault(key, 0L);
    }

    public List<AEKey> itemCandidates() {
        return this.itemCandidates;
    }

    public void setExposureStats(ExposureStats exposureStats) {
        this.exposureStats = exposureStats;
        this.exposureDirty = false;
    }

    public void markExposureDirty() {
        this.exposureDirty = true;
    }

    public ExposureStats exposureStats() {
        return this.exposureStats;
    }

    public boolean exposureDirty() {
        return this.exposureDirty;
    }

    private static long saturatedAdd(long left, long right) {
        long result = left + right;
        return result < 0 ? Long.MAX_VALUE : result;
    }
}
