package com.java.beipuo.ae2virus.infection;

import appeng.api.networking.IGridService;
import appeng.api.stacks.AEKey;
import java.util.Set;

public interface IVirusNetworkService extends IGridService {
    VirusNetworkStats getStats();

    int debugAddVirusAndInfect(VirusClass virusClass);

    int debugRemoveVirusAndClear(VirusClass virusClass);

    boolean isInfected(AEKey key);

    Set<AEKey> getInfectedKeys();

    int getInfectionVersion();
}
