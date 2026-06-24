package com.java.beipuo.ae2virus.client.infection;

import appeng.api.stacks.AEKey;
import com.java.beipuo.ae2virus.network.packet.SyncVirusInfoPacket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientVirusState {
    private static final Map<AEKey, SyncVirusInfoPacket.T1VirusInfo> T1_VIRUSES = new ConcurrentHashMap<>();

    private ClientVirusState() {
    }

    public static void replaceT1Viruses(List<SyncVirusInfoPacket.T1VirusInfo> viruses) {
        T1_VIRUSES.clear();
        for (SyncVirusInfoPacket.T1VirusInfo virus : viruses) {
            T1_VIRUSES.put(virus.target(), virus);
        }
    }

    public static long blockedAmount(AEKey key) {
        SyncVirusInfoPacket.T1VirusInfo virus = T1_VIRUSES.get(key);
        return virus == null ? 0L : virus.blockedAmount();
    }
}
