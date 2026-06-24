package com.java.beipuo.ae2virus.infection;

import net.minecraft.util.RandomSource;

public final class InfectionRisk {
    private InfectionRisk() {
    }

    public static ExposureChances exposureChances(ExposureStats exposure, InfectionConfig config) {
        InfectionConfig.Exposure exposureConfig = config.exposure();
        double cablePressure = exposure.exposedCableFaces() * exposureConfig.cableFaceWeight();
        double machinePressure = exposure.machineExposureWeight();
        double wirelessPressure = exposure.wirelessRangeExposures() * exposureConfig.wirelessRangeWeight();
        double cableChance = exposureChance(cablePressure, config);
        double machineChance = exposureChance(machinePressure, config);
        double wirelessChance = exposureChance(wirelessPressure, config);
        double combinedChance = ProbabilityCurves.union(cableChance, machineChance, wirelessChance);
        return new ExposureChances(cableChance, machineChance, wirelessChance,
                ProbabilityCurves.clamp(combinedChance, 0.0, 1.0));
    }

    public static double exposureChance(ExposureStats exposure, InfectionConfig config) {
        return exposureChances(exposure, config).combined();
    }

    public static double t1AttemptChance(long itemCount, InfectionConfig config) {
        return t1InfectionChance(itemCount, 1.0, config);
    }

    public static double t1InfectionChance(long itemCount, ExposureStats exposure, InfectionConfig config) {
        return t1InfectionChance(itemCount, exposureChance(exposure, config), config);
    }

    public static double t1InfectionChance(long itemCount, double exposureChance, InfectionConfig config) {
        InfectionConfig.T1 t1 = config.t1();
        double baseChance = config.baseChances().t1Infection();
        double quantityPressure = ProbabilityCurves.gompertz(itemCount, t1.initialSuppression(),
                t1.steepness(), t1.itemCountScale());
        return ProbabilityCurves.clamp(baseChance + (1.0 - baseChance) * exposureChance * quantityPressure, 0.0,
                1.0);
    }

    public static double t2InfectionAttemptChance(long targetCount, InfectionConfig config) {
        return t2InfectionChance(targetCount, 1.0, config);
    }

    public static double t2InfectionChance(long targetCount, ExposureStats exposure, InfectionConfig config) {
        return t2InfectionChance(targetCount, exposureChance(exposure, config), config);
    }

    public static double t2InfectionChance(long targetCount, double exposureChance, InfectionConfig config) {
        InfectionConfig.T2Infection t2 = config.t2Infection();
        double baseChance = config.baseChances().t2Infection();
        double quantityPressure = ProbabilityCurves.gompertz(targetCount, t2.initialSuppression(),
                t2.steepness(), t2.targetCountScale());
        return ProbabilityCurves.clamp(baseChance + (1.0 - baseChance) * exposureChance * quantityPressure, 0.0,
                1.0);
    }

    public static double t2FusionConversionChance(long infectedItemTypes, int t1Level, InfectionConfig config) {
        double levelMultiplier = virusLevelMultiplier(t1Level);
        InfectionConfig.T2Conversion t2 = config.t2Conversion();
        double curve = curve(infectedItemTypes, t2.fusionCurve());
        return ProbabilityCurves.clamp(t2.fusionFactor() * curve * levelMultiplier, 0.0, 1.0);
    }

    public static double t2SpecializedConversionChance(long infectedTagSeedTypes, int t1Level,
            InfectionConfig config) {
        double levelMultiplier = virusLevelMultiplier(t1Level);
        InfectionConfig.T2Conversion t2 = config.t2Conversion();
        double curve = curve(infectedTagSeedTypes, t2.specializedCurve());
        return ProbabilityCurves.clamp(t2.specializedFactor() * curve * levelMultiplier, 0.0, 1.0);
    }

    public static double t2DedicatedConversionChance(int t1Level, InfectionConfig config) {
        double levelMultiplier = virusLevelMultiplier(t1Level);
        InfectionConfig.T2Conversion t2 = config.t2Conversion();
        double curve = t2.dedicatedBaseChance()
                + (1.0 - t2.dedicatedBaseChance()) * Math.pow(levelMultiplier, t2.dedicatedLevelExponent());
        return ProbabilityCurves.clamp(t2.dedicatedFactor() * curve, 0.0, 1.0);
    }

    public static double t2OtherConversionChance(long resourceAmount, InfectionConfig config) {
        InfectionConfig.T2Conversion t2 = config.t2Conversion();
        double curve = curve(t2.otherFactor() * resourceAmount, t2.otherCurve());
        return ProbabilityCurves.clamp(curve, 0.0, 1.0);
    }

    public static double anyT2ConversionChance(double fusionChance, double specializedChance, double dedicatedChance,
            double otherChance) {
        return ProbabilityCurves.union(fusionChance, specializedChance, dedicatedChance, otherChance);
    }

    public static double t3ConversionRisk(long fusionCount, double fusionEvolutionLevel,
            long specializedCount, double specializedEvolutionLevel,
            long dedicatedCount, double dedicatedEvolutionLevel,
            long energyCount, double energyEvolutionLevel,
            long lightningCount, double lightningEvolutionLevel,
            long sourceCount, double sourceEvolutionLevel,
            long soulCount, double soulEvolutionLevel,
            InfectionConfig config) {
        InfectionConfig.VariantFactors factors = config.t3().variantFactors();
        return fusionCount * factors.fusion() * fusionEvolutionLevel
                + specializedCount * factors.specialized() * specializedEvolutionLevel
                + dedicatedCount * factors.dedicated() * dedicatedEvolutionLevel
                + energyCount * factors.energy() * energyEvolutionLevel
                + lightningCount * factors.lightning() * lightningEvolutionLevel
                + sourceCount * factors.source() * sourceEvolutionLevel
                + soulCount * factors.soul() * soulEvolutionLevel;
    }

    public static double t3ConversionChance(double conversionRisk, InfectionConfig config) {
        InfectionConfig.T3 t3 = config.t3();
        double baseChance = config.baseChances().t3Conversion();
        double curve = ProbabilityCurves.gompertz(conversionRisk, t3.initialSuppression(),
                t3.steepness(), t3.riskScale());
        return ProbabilityCurves.clamp(baseChance + (1.0 - baseChance) * curve, 0.0, 1.0);
    }

    public static double spreadSpeedMultiplier(long infectedAmount, int virusLevel, InfectionConfig config) {
        InfectionConfig.Spread spread = config.spread();
        double amountPressure = ProbabilityCurves.expSaturation(infectedAmount, spread.infectedAmountScale());
        double levelMultiplier = spreadLevelMultiplier(virusLevel, spread);
        double multiplier = 1.0 + spread.acceleration() * amountPressure * levelMultiplier;
        return ProbabilityCurves.clamp(multiplier, 1.0, spread.maxSpeedMultiplier());
    }

    public static int spreadIntervalTicks(int baseIntervalTicks, long infectedAmount, int virusLevel,
            InfectionConfig config) {
        double multiplier = spreadSpeedMultiplier(infectedAmount, virusLevel, config);
        int accelerated = (int) Math.floor(baseIntervalTicks / multiplier);
        return (int) ProbabilityCurves.clamp(accelerated, config.spread().minIntervalTicks(), baseIntervalTicks);
    }

    public static long spreadGrowthAmount(long infectedAmount, int virusLevel, InfectionConfig config) {
        double multiplier = spreadSpeedMultiplier(infectedAmount, virusLevel, config);
        return Math.max(1L, (long) Math.floor(multiplier));
    }

    public static InfectionRoll roll(double attemptChance, ExposureStats exposure, RandomSource random,
            InfectionConfig config) {
        double attempt = ProbabilityCurves.clamp(attemptChance, 0.0, 1.0);
        if (random.nextDouble() >= attempt) {
            return InfectionRoll.noAttempt(attempt);
        }

        double success = exposureChance(exposure, config);
        if (random.nextDouble() >= success) {
            return InfectionRoll.failedAttempt(attempt, success);
        }

        return InfectionRoll.success(attempt, success);
    }

    private static double exposureChance(double exposurePressure, InfectionConfig config) {
        InfectionConfig.Exposure exposure = config.exposure();
        double saturated = ProbabilityCurves.expSaturation(exposurePressure, exposure.scale());
        return ProbabilityCurves.clamp(saturated, 0.0, exposure.maxSuccessChance());
    }

    private static double virusLevelMultiplier(int virusLevel) {
        return ProbabilityCurves.clamp(virusLevel, 1.0, 5.0) / 5.0;
    }

    private static double spreadLevelMultiplier(int virusLevel, InfectionConfig.Spread spread) {
        double maxLevel = Math.max(2.0, spread.maxVirusLevel());
        double clampedLevel = ProbabilityCurves.clamp(virusLevel, 1.0, maxLevel);
        double normalized = (clampedLevel - 1.0) / (maxLevel - 1.0);
        return 1.0 + spread.levelInfluence() * Math.pow(normalized, spread.levelExponent());
    }

    private static double curve(double value, InfectionConfig.Curve curve) {
        return ProbabilityCurves.gompertz(value, curve.initialSuppression(), curve.steepness(), curve.scale());
    }

    public record ExposureChances(double cable, double machine, double wireless, double combined) {
    }
}
