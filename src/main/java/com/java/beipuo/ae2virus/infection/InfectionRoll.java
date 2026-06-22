package com.java.beipuo.ae2virus.infection;

public record InfectionRoll(Result result, double attemptChance, double successChance) {
    public enum Result {
        NO_ATTEMPT,
        FAILED_ATTEMPT,
        SUCCESS
    }

    public static InfectionRoll noAttempt(double attemptChance) {
        return new InfectionRoll(Result.NO_ATTEMPT, attemptChance, 0.0);
    }

    public static InfectionRoll failedAttempt(double attemptChance, double successChance) {
        return new InfectionRoll(Result.FAILED_ATTEMPT, attemptChance, successChance);
    }

    public static InfectionRoll success(double attemptChance, double successChance) {
        return new InfectionRoll(Result.SUCCESS, attemptChance, successChance);
    }
}
