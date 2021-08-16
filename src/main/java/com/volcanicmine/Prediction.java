package com.volcanicmine;

import lombok.Getter;

public class Prediction {
    @Getter
    private final int lowGuess, highGuess;

    @Getter
    private final boolean lowCorrect, highCorrect;

    @Getter
    private final boolean aFailing;

    public Prediction(int lowGuess, boolean lowCorrect, int highGuess, boolean highCorrect,
            boolean aFailing) {
        this.lowGuess = lowGuess;
        this.lowCorrect = lowCorrect;
        this.highGuess = highGuess;
        this.highCorrect = highCorrect;
        this.aFailing = aFailing;
    }

    public Prediction add(Prediction prediction) {
        return new Prediction(lowGuess + prediction.lowGuess, prediction.lowCorrect,
                highGuess + prediction.highGuess, prediction.highCorrect, prediction.aFailing);
    }

    @Override
    public String toString() {
        if (lowCorrect && highCorrect) {
            return "(" + lowGuess + " | " + highGuess + ")%";
        }
        if (lowCorrect) {
            return "(" + lowGuess + ")%";
        }
        if (highCorrect) {
            return "(" + highGuess + ")%";
        }
        return "(?)%";
    }
}
