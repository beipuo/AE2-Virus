package com.java.beipuo.ae2virus.infection;

import appeng.api.stacks.AEKey;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

public final class T2VirusState {
    private final T2VirusKind kind;
    private final ResourceLocation targetId;
    private final Map<AEKey, Long> blockedAmounts = new LinkedHashMap<>();
    private long experience;
    private int level;

    public T2VirusState(T2VirusKind kind, ResourceLocation targetId, long experience) {
        this.kind = kind;
        this.targetId = targetId;
        this.experience = Math.max(0L, experience);
        this.level = T1VirusState.levelForExperience(this.experience);
    }

    public T2VirusKind kind() {
        return this.kind;
    }

    public ResourceLocation targetId() {
        return this.targetId;
    }

    public long experience() {
        return this.experience;
    }

    public int level() {
        return this.level;
    }

    public long blockedAmount(AEKey key) {
        return this.blockedAmounts.getOrDefault(key, 0L);
    }

    public long totalBlockedAmount() {
        long total = 0L;
        for (long amount : this.blockedAmounts.values()) {
            total = saturatedAdd(total, amount);
        }
        return total;
    }

    public int infectedTargetCount() {
        return this.blockedAmounts.size();
    }

    public List<AEKey> targets() {
        return List.copyOf(this.blockedAmounts.keySet());
    }

    public Map<AEKey, Long> blockedAmounts() {
        return Map.copyOf(this.blockedAmounts);
    }

    public boolean addBlockedAmount(AEKey target, long amount) {
        if (target == null || amount <= 0) {
            return false;
        }

        this.blockedAmounts.merge(target, amount, T2VirusState::saturatedAdd);
        this.experience = saturatedAdd(this.experience, amount);
        this.level = T1VirusState.levelForExperience(this.experience);
        return true;
    }

    public boolean mergeTarget(AEKey target, long blockedAmount) {
        if (target == null || blockedAmount <= 0) {
            return false;
        }
        long current = this.blockedAmounts.getOrDefault(target, 0L);
        long merged = Math.max(current, blockedAmount);
        if (merged == current) {
            return false;
        }
        this.blockedAmounts.put(target, merged);
        return true;
    }

    public boolean mergeExperience(long experience) {
        long newExperience = Math.max(this.experience, Math.max(0L, experience));
        int newLevel = T1VirusState.levelForExperience(newExperience);
        boolean changed = newExperience != this.experience || newLevel != this.level;
        this.experience = newExperience;
        this.level = newLevel;
        return changed;
    }

    private static long saturatedAdd(long left, long right) {
        long result = left + right;
        return result < 0 ? Long.MAX_VALUE : result;
    }
}
