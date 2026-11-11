package com.hiresphere.match;

public enum FitLabel {
    STRONG,
    GOOD,
    PARTIAL,
    LOW,
    UNKNOWN;

    public static FitLabel of(Integer score) {
        if (score == null) {
            return UNKNOWN;
        }
        if (score >= 75) {
            return STRONG;
        }
        if (score >= 50) {
            return GOOD;
        }
        if (score >= 25) {
            return PARTIAL;
        }
        return LOW;
    }
}