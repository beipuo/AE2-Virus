package com.java.beipuo.ae2virus.infection;

import appeng.api.stacks.AEKey;

public final class T1VirusState {
    private final AEKey target;
    private long blockedAmount;
    private long experience;
    private int level;

    public T1VirusState(AEKey target, long blockedAmount, long experience) {
        this.target = target;
        this.blockedAmount = Math.max(0L, blockedAmount);
        this.experience = Math.max(0L, experience);
        this.level = levelForExperience(this.experience);
    }

    public AEKey target() {
        return this.target;
    }

    public long blockedAmount() {
        return this.blockedAmount;
    }

    public long experience() {
        return this.experience;
    }

    public int level() {
        return this.level;
    }

    public boolean addBlockedAmount(long amount) {
        if (amount <= 0) {
            return false;
        }

        this.blockedAmount += amount;
        this.experience += amount;
        int newLevel = levelForExperience(this.experience);
        boolean changed = newLevel != this.level || amount > 0;
        this.level = newLevel;
        return changed;
    }

    public boolean merge(long blockedAmount, long experience) {
        long newBlockedAmount = Math.max(this.blockedAmount, Math.max(0L, blockedAmount));
        long newExperience = Math.max(this.experience, Math.max(0L, experience));
        int newLevel = levelForExperience(newExperience);
        boolean changed = newBlockedAmount != this.blockedAmount
                || newExperience != this.experience
                || newLevel != this.level;
        this.blockedAmount = newBlockedAmount;
        this.experience = newExperience;
        this.level = newLevel;
        return changed;
    }

    public static int levelForExperience(long experience) {
        if (experience >= 256_000L) {
            return 5;
        }
        if (experience >= 64_000L) {
            return 4;
        }
        if (experience >= 16_000L) {
            return 3;
        }
        if (experience >= 1_000L) {
            return 2;
        }
        return 1;
    }
}
