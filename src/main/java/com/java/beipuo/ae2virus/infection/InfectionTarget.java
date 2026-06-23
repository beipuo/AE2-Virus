package com.java.beipuo.ae2virus.infection;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

public sealed interface InfectionTarget
        permits InfectionTarget.ItemTarget, InfectionTarget.BroadSpectrumTarget, InfectionTarget.None {
    record ItemTarget(Item item) implements InfectionTarget {
    }

    record BroadSpectrumTarget(
            BroadSpectrumVariant broadVariant,
            ResourceLocation tagId,
            long diskId,
            long driveId,
            int targetedSeedTypes)
            implements InfectionTarget {
        public static final long NETWORK_DISK_ID = 0L;
        public static final long NETWORK_DRIVE_ID = 0L;

        public static BroadSpectrumTarget tag(ResourceLocation tagId, int targetedSeedTypes) {
            return new BroadSpectrumTarget(BroadSpectrumVariant.TAG, tagId, NETWORK_DISK_ID, NETWORK_DRIVE_ID,
                    targetedSeedTypes);
        }

        public static BroadSpectrumTarget disk(long diskId) {
            return new BroadSpectrumTarget(BroadSpectrumVariant.DISK, null, diskId, NETWORK_DRIVE_ID, 0);
        }

        public static BroadSpectrumTarget drive(long driveId) {
            return new BroadSpectrumTarget(BroadSpectrumVariant.DRIVE, null, NETWORK_DISK_ID, driveId, 0);
        }
    }

    enum None implements InfectionTarget {
        INSTANCE
    }
}
