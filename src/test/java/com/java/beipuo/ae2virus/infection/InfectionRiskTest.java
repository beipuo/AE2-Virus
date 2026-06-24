package com.java.beipuo.ae2virus.infection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class InfectionRiskTest {
    private static final double EPSILON = 1.0e-12;
    private static final InfectionConfig CONFIG = InfectionConfig.defaults();

    @Test
    void baseCurvesMatchFormulaBoundaries() {
        assertEquals(0.0, ProbabilityCurves.expSaturation(0.0, 32.0), EPSILON);
        assertEquals(0.0, ProbabilityCurves.gompertz(0.0, 6.0, 4.0, 4096.0), EPSILON);
        assertEquals(0.28, ProbabilityCurves.union(0.1, 0.2), EPSILON);
    }

    @Test
    void exposureCombinesCableMachineAndWireless() {
        var exposure = new ExposureStats(2, 1, 3.0);
        var chances = InfectionRisk.exposureChances(exposure, CONFIG);

        assertTrue(chances.cable() > 0.0);
        assertTrue(chances.machine() > 0.0);
        assertTrue(chances.wireless() > 0.0);
        assertEquals(
                ProbabilityCurves.union(chances.cable(), chances.machine(), chances.wireless()),
                chances.combined(),
                EPSILON);
        assertTrue(chances.combined() <= 1.0);
    }

    @Test
    void t1InfectionKeepsBaseChanceAndGrowsWithItemCount() {
        double zero = InfectionRisk.t1InfectionChance(0L, 1.0, CONFIG);
        double medium = InfectionRisk.t1InfectionChance(4096L, 1.0, CONFIG);
        double large = InfectionRisk.t1InfectionChance(4096L * 8L, 1.0, CONFIG);

        assertEquals(CONFIG.baseChances().t1Infection(), zero, EPSILON);
        assertTrue(medium > zero);
        assertTrue(large > medium);
        assertTrue(large <= 1.0);
    }

    @Test
    void t2ConversionUsesConfiguredFactorOrderingAndLevelMultiplier() {
        double fusionLow = InfectionRisk.t2FusionConversionChance(8L, 1, CONFIG);
        double fusionHigh = InfectionRisk.t2FusionConversionChance(8L, 5, CONFIG);
        double specializedLowSeed = InfectionRisk.t2SpecializedConversionChance(1L, 5, CONFIG);
        double specializedHighSeed = InfectionRisk.t2SpecializedConversionChance(8L, 5, CONFIG);
        double dedicatedLow = InfectionRisk.t2DedicatedConversionChance(1, CONFIG);
        double dedicatedHigh = InfectionRisk.t2DedicatedConversionChance(5, CONFIG);

        assertTrue(CONFIG.t2Conversion().fusionFactor() > CONFIG.t2Conversion().dedicatedFactor());
        assertTrue(CONFIG.t2Conversion().dedicatedFactor() > CONFIG.t2Conversion().otherFactor());
        assertTrue(CONFIG.t2Conversion().otherFactor() > CONFIG.t2Conversion().specializedFactor());
        assertTrue(fusionHigh > fusionLow);
        assertTrue(specializedHighSeed > specializedLowSeed);
        assertTrue(dedicatedHigh > dedicatedLow);
    }

    @Test
    void t2AndT3ProbabilitiesAreClampedAndMonotonic() {
        double t2Small = InfectionRisk.t2InfectionAttemptChance(1L, CONFIG);
        double t2Large = InfectionRisk.t2InfectionAttemptChance(240000L, CONFIG);
        double t3Small = InfectionRisk.t3ConversionChance(1.0, CONFIG);
        double t3Large = InfectionRisk.t3ConversionChance(64.0, CONFIG);

        assertTrue(t2Large > t2Small);
        assertTrue(t2Large <= 1.0);
        assertTrue(t3Large > t3Small);
        assertTrue(t3Large <= 1.0);
    }

    @Test
    void anyT2ConversionUsesProbabilityUnion() {
        double chance = InfectionRisk.anyT2ConversionChance(0.1, 0.2, 0.3, 0.4);

        assertEquals(1.0 - 0.9 * 0.8 * 0.7 * 0.6, chance, EPSILON);
    }

    @Test
    void spreadSpeedGrowsWithInfectedAmountAndLevel() {
        double empty = InfectionRisk.spreadSpeedMultiplier(0L, 1, CONFIG);
        double manyLowLevel = InfectionRisk.spreadSpeedMultiplier(4096L, 1, CONFIG);
        double manyHighLevel = InfectionRisk.spreadSpeedMultiplier(4096L, 5, CONFIG);

        assertEquals(1.0, empty, EPSILON);
        assertTrue(manyLowLevel > empty);
        assertTrue(manyHighLevel > manyLowLevel);
        assertTrue(manyHighLevel <= CONFIG.spread().maxSpeedMultiplier());
    }

    @Test
    void spreadIntervalIsClampedBetweenMinimumAndBase() {
        int baseInterval = CONFIG.runtime().riskCheckIntervalTicks();

        assertEquals(baseInterval, InfectionRisk.spreadIntervalTicks(baseInterval, 0L, 1, CONFIG));
        int accelerated = InfectionRisk.spreadIntervalTicks(baseInterval, 4096L, 5, CONFIG);
        int fastest = InfectionRisk.spreadIntervalTicks(baseInterval, Long.MAX_VALUE, 5, CONFIG);

        assertTrue(accelerated < baseInterval);
        assertTrue(fastest <= accelerated);
        assertTrue(fastest >= CONFIG.spread().minIntervalTicks());
    }

    @Test
    void spreadGrowthAmountNeverDropsBelowOneAndGrowsWithPressure() {
        long emptyGrowth = InfectionRisk.spreadGrowthAmount(0L, 1, CONFIG);
        long highGrowth = InfectionRisk.spreadGrowthAmount(Long.MAX_VALUE, 5, CONFIG);

        assertEquals(1L, emptyGrowth);
        assertTrue(highGrowth > emptyGrowth);
        assertTrue(highGrowth <= (long) CONFIG.spread().maxSpeedMultiplier());
    }
}
