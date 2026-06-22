package com.java.beipuo.ae2virus.infection;

import appeng.api.stacks.AEKey;
import java.util.List;

public record VirusNetworkStats(int storedKeyCount, long totalStoredAmount, List<AEKey> itemCandidates) {
    public static final VirusNetworkStats EMPTY = new VirusNetworkStats(0, 0L, List.of());
}
