package com.java.beipuo.ae2virus.storage;

import appeng.api.behaviors.ContainerItemStrategy;
import appeng.api.stacks.AEKeyTypes;
import com.java.beipuo.ae2virus.item.DataStreamCapsuleStrategy;

public final class AVKeyTypes {
    private static boolean registered;
    private static boolean containerStrategiesRegistered;

    private AVKeyTypes() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        AEKeyTypes.register(DataStreamKeyType.TYPE);
    }

    public static synchronized void registerContainerStrategies() {
        if (containerStrategiesRegistered) {
            return;
        }
        containerStrategiesRegistered = true;
        ContainerItemStrategy.register(DataStreamKeyType.TYPE, DataStreamKey.class, DataStreamCapsuleStrategy.INSTANCE);
    }
}
