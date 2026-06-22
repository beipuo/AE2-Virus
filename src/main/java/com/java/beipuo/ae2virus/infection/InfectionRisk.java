package com.java.beipuo.ae2virus.infection;

import net.minecraft.util.RandomSource;

public final class InfectionRisk {
    private InfectionRisk() {
    }

    public static double attemptChance(VirusClass virusClass, InfectionTarget target,
            VirusNetworkRiskCache cache, InfectionConfig config) {
        double pressure = pressure(virusClass, target, cache, config);
        if (pressure <= 0.0) {
            return 0.0;
        }

        double normalized = ProbabilityCurves.expSaturation(pressure);
        return clamp(
                config.minAttemptChance() + normalized * (config.maxAttemptChance() - config.minAttemptChance()),
                0.0,
                config.maxAttemptChance());
    }

    public static double successChance(ExposureStats exposure, InfectionConfig config) {
        double cableExposure = exposure.exposedCableFaces() * config.cableFaceWeight()
                + exposure.wirelessRangeExposures() * config.wirelessRangeWeight();
        double weightedExposure = cableExposure + exposure.machineExposureWeight();
        if (weightedExposure <= 0.0) {
            return 0.0;
        }

        double breach = ProbabilityCurves.expSaturation(weightedExposure, config.exposureScale());
        return clamp(breach, 0.0, config.maxSuccessChance());
    }

    public static InfectionRoll roll(VirusClass virusClass, InfectionTarget target,
            VirusNetworkRiskCache cache, ExposureStats exposure, RandomSource random, InfectionConfig config) {
        double attempt = attemptChance(virusClass, target, cache, config);
        if (random.nextDouble() >= attempt) {
            return InfectionRoll.noAttempt(attempt);
        }

        double success = successChance(exposure, config);
        if (random.nextDouble() >= success) {
            return InfectionRoll.failedAttempt(attempt, success);
        }

        return InfectionRoll.success(attempt, success);
    }

    private static double pressure(VirusClass virusClass, InfectionTarget target,
            VirusNetworkRiskCache cache, InfectionConfig config) {
        return switch (virusClass) {
            case TARGETED -> targetedPressure(target, cache, config);
            case BROAD_SPECTRUM -> broadSpectrumPressure(target, cache, config);
            case SYSTEMIC -> systemicPressure(cache, config);
            case POLYMORPHIC -> cache.blacklistedItemCount() / config.polymorphicBlacklistCountScale();
        };
    }

    private static double targetedPressure(InfectionTarget target, VirusNetworkRiskCache cache,
            InfectionConfig config) {
        if (target instanceof InfectionTarget.ItemTarget itemTarget) {
            return cache.itemCount(itemTarget.item()) / config.targetedItemCountScale();
        }
        return 0.0;
    }

    private static double broadSpectrumPressure(InfectionTarget target, VirusNetworkRiskCache cache,
            InfectionConfig config) {
        double targetedVirusPressure = cache.virusCount(VirusClass.TARGETED) / config.broadTargetedVirusCountScale();

        if (!(target instanceof InfectionTarget.BroadSpectrumTarget broadTarget)) {
            return 0.0;
        }

        return switch (broadTarget.broadVariant()) {
            case TAG -> tagBroadPressure(broadTarget, cache, config, targetedVirusPressure);
            case DISK -> diskBroadPressure(broadTarget, cache, config, targetedVirusPressure);
            case DRIVE -> driveBroadPressure(broadTarget, cache, config, targetedVirusPressure);
        };
    }

    private static double tagBroadPressure(InfectionTarget.BroadSpectrumTarget target, VirusNetworkRiskCache cache,
            InfectionConfig config, double targetedVirusPressure) {
        if (target.tagId() == null || !cache.hasEveryItemInTag(target.tagId())) {
            return 0.0;
        }

        double tagPressure = cache.tagItemCount(target.tagId()) / config.broadTagItemCountScale();
        return tagPressure * config.broadTagWeight()
                + targetedVirusPressure * config.broadTargetedVirusWeight();
    }

    private static double diskBroadPressure(InfectionTarget.BroadSpectrumTarget target, VirusNetworkRiskCache cache,
            InfectionConfig config, double targetedVirusPressure) {
        double diskPressure = cache.diskUsedBytes(target.diskId()) / config.broadDiskUsedBytesScale();
        return diskPressure * config.broadDiskWeight()
                + targetedVirusPressure * config.broadTargetedVirusWeight();
    }

    private static double driveBroadPressure(InfectionTarget.BroadSpectrumTarget target, VirusNetworkRiskCache cache,
            InfectionConfig config, double targetedVirusPressure) {
        double infectedDiskPressure = cache.infectedDiskCount(target.driveId())
                / config.broadDriveInfectedDiskCountScale();
        return infectedDiskPressure * config.broadDriveWeight()
                + targetedVirusPressure * config.broadTargetedVirusWeight();
    }

    private static double systemicPressure(VirusNetworkRiskCache cache, InfectionConfig config) {
        double totalBytesPressure = cache.totalBytes() / config.systemicTotalBytesScale();
        double broadVirusPressure = cache.virusCount(VirusClass.BROAD_SPECTRUM) / config.systemicBroadVirusCountScale();

        return totalBytesPressure * config.systemicTotalBytesWeight()
                + broadVirusPressure * config.systemicBroadVirusWeight();
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
