package com.java.beipuo.ae2virus.infection;

public record InfectionConfig(
        BaseChances baseChances,
        Exposure exposure,
        T1 t1,
        T2Infection t2Infection,
        T2Conversion t2Conversion,
        T3 t3,
        Spread spread,
        Runtime runtime) {
    public static InfectionConfig defaults() {
        return new InfectionConfig(
                BaseChances.defaults(),
                Exposure.defaults(),
                T1.defaults(),
                T2Infection.defaults(),
                T2Conversion.defaults(),
                T3.defaults(),
                Spread.defaults(),
                Runtime.defaults());
    }

    public record BaseChances(
            double t1Infection,
            double t2Infection,
            double t3Conversion) {
        public static BaseChances defaults() {
            return new BaseChances(
                    0.0001,
                    0.00005,
                    0.0);
        }
    }

    public record Exposure(
            double maxSuccessChance,
            double scale,
            double cableFaceWeight,
            double machineFaceWeight,
            double wirelessRangeWeight) {
        public static Exposure defaults() {
            return new Exposure(
                    0.75,
                    32.0,
                    1.0,
                    2.0,
                    4.0);
        }
    }

    public record T1(
            double initialSuppression,
            double steepness,
            double itemCountScale) {
        public static T1 defaults() {
            return new T1(
                    6.0,
                    4.0,
                    4096.0);
        }
    }

    public record T2Infection(
            double initialSuppression,
            double steepness,
            double targetCountScale) {
        public static T2Infection defaults() {
            return new T2Infection(
                    6.0,
                    4.0,
                    120000.0);
        }
    }

    public record T2Conversion(
            Curve fusionCurve,
            Curve specializedCurve,
            Curve otherCurve,
            double fusionFactor,
            double dedicatedFactor,
            double otherFactor,
            double specializedFactor,
            double dedicatedBaseChance,
            double dedicatedLevelExponent) {
        public static T2Conversion defaults() {
            return new T2Conversion(
                    new Curve(6.0, 4.0, 13.2),
                    new Curve(6.0, 4.0, 13.2),
                    new Curve(6.0, 4.0, 120000.0),
                    1.0,
                    0.85,
                    0.70,
                    0.55,
                    0.001,
                    2.0);
        }
    }

    public record T3(
            double initialSuppression,
            double steepness,
            double riskScale,
            VariantFactors variantFactors) {
        public static T3 defaults() {
            return new T3(
                    7.0,
                    3.0,
                    16.0,
                    VariantFactors.defaults());
        }
    }

    public record VariantFactors(
            double fusion,
            double specialized,
            double dedicated,
            double energy,
            double lightning,
            double source,
            double soul) {
        public static VariantFactors defaults() {
            return new VariantFactors(
                    1.0,
                    1.2,
                    1.5,
                    0.8,
                    1.0,
                    1.0,
                    1.0);
        }
    }

    public record Spread(
            double infectedAmountScale,
            double levelInfluence,
            double levelExponent,
            double acceleration,
            double maxSpeedMultiplier,
            int maxVirusLevel,
            int minIntervalTicks) {
        public static Spread defaults() {
            return new Spread(
                    4096.0,
                    1.0,
                    1.5,
                    1.0,
                    4.0,
                    5,
                    20);
        }
    }

    public record Runtime(
            int riskCheckIntervalTicks,
            int maxRiskChecksPerTick,
            int maxCandidatesPerRefresh) {
        public static Runtime defaults() {
            return new Runtime(
                    20 * 20,
                    4,
                    256);
        }
    }

    public record Curve(
            double initialSuppression,
            double steepness,
            double scale) {
    }
}
