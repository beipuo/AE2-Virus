package com.java.beipuo.ae2virus.infection;

import appeng.api.networking.GridServices;

public final class AVGridServices {
    private static boolean registered;

    private AVGridServices() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        GridServices.register(IVirusNetworkService.class, VirusNetworkService.class);
        registered = true;
    }
}
