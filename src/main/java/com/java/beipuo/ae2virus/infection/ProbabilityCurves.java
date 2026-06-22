package com.java.beipuo.ae2virus.infection;

public final class ProbabilityCurves {
    private ProbabilityCurves() {
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
}
