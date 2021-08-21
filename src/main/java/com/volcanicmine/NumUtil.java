package com.volcanicmine;

public class NumUtil {
    public static int clamp(int value, int minValue, int maxValue) {
        return Math.max(Math.min(value, maxValue), minValue);
    }

    public static int clamp(int value) {
        return clamp(value, 0, 100);
    }

    public static boolean isBetweenIncl(int value, int minValue, int maxValue) {
        return value >= minValue && value <= maxValue;
    }

    public static boolean isBetweenExcl(int value, int minValue, int maxValue) {
        return value > minValue && value <= maxValue;
    }

    public static boolean isEither(int value, int firstValue, int secondValue) {
        return value == firstValue || value == secondValue;
    }
}
