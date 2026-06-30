package com.java.beipuo.ae2virus.storage;

import appeng.api.stacks.AEKeyTypes;

public final class AVKeyTypes {
    private static boolean registered;

    private AVKeyTypes() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        AEKeyTypes.register(DataStreamKeyType.TYPE);
    }
}
