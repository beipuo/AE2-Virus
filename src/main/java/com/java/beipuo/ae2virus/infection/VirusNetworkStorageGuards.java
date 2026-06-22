package com.java.beipuo.ae2virus.infection;

import appeng.api.stacks.AEKey;
import appeng.api.storage.MEStorage;
import java.util.Map;
import java.util.WeakHashMap;

public final class VirusNetworkStorageGuards {
    private static final Map<MEStorage, IVirusNetworkService> SERVICES = new WeakHashMap<>();

    private VirusNetworkStorageGuards() {
    }

    public static synchronized void register(MEStorage storage, IVirusNetworkService service) {
        SERVICES.put(storage, service);
    }

    public static synchronized boolean blocksExtraction(MEStorage storage, AEKey key) {
        IVirusNetworkService service = SERVICES.get(storage);
        return service != null && service.isInfected(key);
    }
}
