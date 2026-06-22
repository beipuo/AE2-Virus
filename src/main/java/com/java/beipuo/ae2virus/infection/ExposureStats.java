package com.java.beipuo.ae2virus.infection;

public record ExposureStats(int exposedCableFaces, int wirelessRangeExposures, double machineExposureWeight) {
    public static final ExposureStats NONE = new ExposureStats(0, 0, 0.0);

    public boolean hasExposure() {
        return exposedCableFaces > 0 || wirelessRangeExposures > 0 || machineExposureWeight > 0.0;
    }
}
