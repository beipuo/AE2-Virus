package com.java.beipuo.ae2virus.infection;

public final class ProbabilityCurves {
    private ProbabilityCurves() {
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static double gompertz(double value, double initialSuppression, double steepness, double scale) {
        if (value <= 0.0 || scale <= 0.0) {
            return 0.0;
        }
        return Math.exp(-initialSuppression * Math.exp(-steepness * value / scale));
    }

    public static double expSaturation(double value) {
        return expSaturation(value, 1.0);
    }

    public static double expSaturation(double value, double scale) {
        if (value <= 0.0 || scale <= 0.0) {
            return 0.0;
        }
        return 1.0 - Math.exp(-value / scale);
    }

    public static double union(double... probabilities) {
        double product = 1.0;
        for (double probability : probabilities) {
            product *= 1.0 - clamp(probability, 0.0, 1.0);
        }
        return 1.0 - product;
    }
}
