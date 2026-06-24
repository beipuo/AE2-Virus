package com.java.beipuo.ae2virus.client.infection;

import appeng.api.stacks.AEKey;
import com.java.beipuo.ae2virus.network.packet.SyncVirusInfoPacket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientVirusState {
    private static final Map<AEKey, SyncVirusInfoPacket.T1VirusInfo> T1_VIRUSES = new ConcurrentHashMap<>();
    private static final Map<AEKey, Long> T2_BLOCKED_AMOUNTS = new ConcurrentHashMap<>();
    private static final Map<AEKey, Long> T3_BLOCKED_AMOUNTS = new ConcurrentHashMap<>();

    private ClientVirusState() {
    }

    public static void replaceViruses(SyncVirusInfoPacket packet) {
        T1_VIRUSES.clear();
        T2_BLOCKED_AMOUNTS.clear();
        T3_BLOCKED_AMOUNTS.clear();
        for (SyncVirusInfoPacket.T1VirusInfo virus : packet.t1Viruses()) {
            T1_VIRUSES.put(virus.target(), virus);
        }
        for (SyncVirusInfoPacket.T2VirusInfo virus : packet.t2Viruses()) {
            for (SyncVirusInfoPacket.T2TargetInfo target : virus.targets()) {
                T2_BLOCKED_AMOUNTS.merge(target.target(), target.blockedAmount(), ClientVirusState::saturatedAdd);
            }
        }
        for (SyncVirusInfoPacket.T3VirusInfo virus : packet.t3Viruses()) {
            for (SyncVirusInfoPacket.T3TargetInfo target : virus.targets()) {
                T3_BLOCKED_AMOUNTS.merge(target.target(), target.blockedAmount(), ClientVirusState::saturatedAdd);
            }
        }
    }

    public static long blockedAmount(AEKey key) {
        SyncVirusInfoPacket.T1VirusInfo virus = T1_VIRUSES.get(key);
        long t1Blocked = virus == null ? 0L : virus.blockedAmount();
        long t1AndT2Blocked = saturatedAdd(t1Blocked, T2_BLOCKED_AMOUNTS.getOrDefault(key, 0L));
        return saturatedAdd(t1AndT2Blocked, T3_BLOCKED_AMOUNTS.getOrDefault(key, 0L));
    }

    private static long saturatedAdd(long left, long right) {
        long result = left + right;
        return result < 0 ? Long.MAX_VALUE : result;
    }
}
