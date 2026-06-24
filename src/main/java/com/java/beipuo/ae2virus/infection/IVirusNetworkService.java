package com.java.beipuo.ae2virus.infection;

import appeng.api.networking.IGridService;
import appeng.api.stacks.AEKey;
import java.util.List;

public interface IVirusNetworkService extends IGridService {
    VirusNetworkStats getStats();

    List<T1VirusState> t1Viruses();

    List<T2VirusState> t2Viruses();

    long blockedAmount(AEKey key);

    long allowedExtraction(AEKey key, long requestedAmount);

    int getInfectionVersion();
}
