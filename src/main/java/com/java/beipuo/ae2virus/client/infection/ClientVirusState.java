package com.java.beipuo.ae2virus.client.infection;

import appeng.api.stacks.AEKey;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientVirusState {
    private static final Set<AEKey> INFECTED_KEYS = ConcurrentHashMap.newKeySet();

    private ClientVirusState() {
    }

    public static void replaceInfectedKeys(Set<AEKey> keys) {
        INFECTED_KEYS.clear();
        INFECTED_KEYS.addAll(keys);
    }

    public static boolean isInfected(AEKey key) {
        return INFECTED_KEYS.contains(key);
    }
}
