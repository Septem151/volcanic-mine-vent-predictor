package com.volcanicmine;

import java.util.SortedSet;

public interface Predictor {
    SortedSet<Integer> getPredictions();

    void update(boolean chamberUpdate, boolean stabilityUpdate);

    String getInGameString();

    void reset();
}
