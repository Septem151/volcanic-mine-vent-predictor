package com.volcanicmine;

public class NumUtil {
    public static int clamp(int value, int minValue, int maxValue) {
        return Math.max(Math.min(value, maxValue), minValue);
    }

    public static int clamp(int value) {
        return clamp(value, 0, 100);
    }
}
