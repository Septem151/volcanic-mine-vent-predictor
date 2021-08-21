package com.volcanicmine;

import static com.volcanicmine.NumUtil.clamp;
import static com.volcanicmine.NumUtil.isBetweenIncl;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class APredictor implements Predictor {
    private Mine mine;
    private List<ChamberUpdate> updates;
    private SortedSet<Integer> predictions;

    private String inGameString;

    // Helper variables
    private Chamber bChamber, cChamber;

    public APredictor(Mine mine) {
        this.mine = mine;
        updates = new ArrayList<ChamberUpdate>();
        predictions = new TreeSet<Integer>();
        if (mine != null) {
            bChamber = mine.getBChamber();
            cChamber = mine.getCChamber();
        }
        inGameString = "(?)%";
    }

    @Override
    public void reset() {
        predictions.clear();
        updates.clear();
        inGameString = "(?)%";
    }

    @Override
    public void update(boolean chamberUpdate, boolean stabilityUpdate) {
        if (!chamberUpdate && !stabilityUpdate) {
            // If there's no update in mine status, don't do anything.
            return;
        }
        SortedSet<Integer> newPredictions = new TreeSet<Integer>();
        boolean aFailing = isChamberFailing();
        boolean aBlocked = mine.getAChamber().isBlocked();
        if (chamberUpdate) {
            if (isBetweenIncl(predictions.size(), 1, 2)) {
                // If we already have a known prediction or prediction is based on
                // stability, we don't need to predict anything.
                // We only need to update the existing predictions.
                for (int prediction : predictions) {
                    // int tempPred = updatePrediction(prediction, aBlocked, aFailing);
                    // if (aFailing && (isBetweenIncl(tempPred, 0, 42)
                    // || isBetweenIncl(tempPred, 58, 100))) {
                    // newPredictions.add(tempPred);
                    // } else if (!aFailing && isBetweenIncl(tempPred, 40, 60)) {
                    // newPredictions.add(tempPred);
                    // }
                    newPredictions.add(updatePrediction(prediction, aBlocked, aFailing));
                }
                if (stabilityUpdate && mine.getStability() != 100) {
                    // If it's also the same tick as a stability update,
                    // we need to make sure the prediction would have actually
                    // produced a valid stability change
                    SortedSet<Integer> validStabPredictions = new TreeSet<Integer>();
                    for (int prediction : newPredictions) {
                        int stabilityDiff = mine.calculateStabilityDiff(prediction);
                        if (isBetweenIncl(Math.abs(mine.getStabilityDiff() - stabilityDiff), 0,
                                2)) {
                            validStabPredictions.add(prediction);
                        }
                    }
                    if (validStabPredictions.size() == 0) {
                        // If somehow we lost both predictions (occurs when B and C
                        // are maxed and the Predictor incorrectly guessed if A
                        // was failing or not, which only occurs if a vent update
                        // happens before a stab update ever occurs), then simply
                        // set the predictions to the calculation of stability.
                        validStabPredictions = generatePredictionSet();
                    }
                    newPredictions = validStabPredictions;
                }
                updatePredictions(newPredictions, aFailing);
                return;
            }
            if (updates.size() == 0) {
                // If this is our first ever update, we need to check if A is Failing
                // and if A is Blocked to determine the range of numbers that the status
                // could be.
                if (aFailing) {
                    newPredictions.addAll(generateRangeSet(0, 2, 38, 41, aBlocked));
                    newPredictions.addAll(generateRangeSet(59, 62, 98, 100, aBlocked));
                } else {
                    newPredictions.addAll(generateRangeSet(40, 42, 58, 60, aBlocked));
                }
                updatePredictions(newPredictions, aFailing);
                return;
            }
            // If this is not our first update, we should assume that we have a range
            // of predictions to work with now, and a previous update's Failure/Blocked
            // status that we can work with.
            ChamberUpdate prevUpdate = updates.get(updates.size() - 1);
            if (!prevUpdate.isFailing() && aFailing) {
                // If we transition from Not Failing -> Failing, we know for certain
                // what the status is based on whether A is Blocked.
                newPredictions.add(aBlocked ? 62 : 38);
                updatePredictions(newPredictions, aFailing);
            } else if (prevUpdate.isFailing() && !aFailing) {
                // If we transition from Failing -> Not Failing, we know that
                // a prediction is 1 of 2 numbers based on whether A is Blocked.
                newPredictions.addAll(generateRangeSet(57, 42, 58, 43, aBlocked));
                updatePredictions(newPredictions, aFailing);
            } else if (prevUpdate.isFailing() && aFailing) {
                // If we transition from Failing -> Failing, we can narrow the range
                // of predictions based on whether A is Blocked.
                newPredictions.addAll(generateRangeSet(0, 4, 36, 41, aBlocked));
                newPredictions.addAll(generateRangeSet(59, 66, 96, 100, aBlocked));
            } else {
                // If we transition from Not Failing -> Not Failing, we can also narrow
                // the range of predictions based on whether A is Blocked.
                newPredictions.addAll(generateRangeSet(40, 43, 57, 60, aBlocked));
            }
            if (updates.size() > 1) {
                log.info("Reached the multiple updates set");
                // If there have been multiple updates, we need to play them back
                // to see which predictions make sense.
                SortedSet<Integer> validPredictions = new TreeSet<Integer>(newPredictions);
                for (int prediction : newPredictions) {
                    int tempPrediction = prediction;
                    for (int i = updates.size() - 1; i >= 0; i--) {
                        ChamberUpdate update = updates.get(i);
                        if (!validPredictions.contains(prediction)) {
                            // If we've already removed the prediction, no need
                            // to process further updates.
                            break;
                        }
                        if (update.isFailing()) {
                            // If the update was a Failing state, we know the prediction
                            // must be within a range that either just failed or is
                            // continuing to fail.
                            if (update.isBlocked() && !(isBetweenIncl(tempPrediction, 2, 41)
                                    || isBetweenIncl(tempPrediction, 62, 100))) {
                                validPredictions.remove(prediction);
                            } else if (!update.isBlocked() && !(isBetweenIncl(tempPrediction, 0, 38)
                                    || isBetweenIncl(tempPrediction, 59, 98))) {
                                validPredictions.remove(prediction);
                            }
                        } else {
                            // If the update was a Not Failing state, we know the
                            // prediction must be within a range that either just
                            // transitioned to not failing or is continuing to not fail.
                            if (update.isBlocked() && !isBetweenIncl(tempPrediction, 42, 62)) {
                                validPredictions.remove(prediction);
                            } else if (!update.isBlocked()
                                    && !isBetweenIncl(tempPrediction, 40, 58)) {
                                validPredictions.remove(prediction);
                            }
                        }
                        // Update the prediction in the reverse direction.
                        tempPrediction = updatePrediction(tempPrediction, !update.isBlocked(),
                                update.isFailing());
                    }

                }
                newPredictions = validPredictions;
            }
        }
        if (stabilityUpdate) {
            if (mine.getStability() != 100 && predictions.size() != 1) {
                // If we're able to predict a new status (stability is not 100%)
                // and the prediction is already not narrowed down to a single value,
                // make a prediction based off the mine's stability (within a range).
                SortedSet<Integer> stabBasedPredictions = generatePredictionSet();
                if (chamberUpdate) {
                    newPredictions =
                            newPredictions.size() > 2 ? stabBasedPredictions : newPredictions;
                } else {
                    newPredictions = predictions.size() > 2 ? stabBasedPredictions : predictions;
                }
                updatePredictions(newPredictions, aFailing);
            }
            return;
        }
        updatePredictions(newPredictions, aFailing);
    }

    private SortedSet<Integer> generateRangeSet(int lowBoundUnblocked, int lowBoundBlocked,
            int highBoundUnblocked, int highBoundBlocked, boolean blocked) {
        int lowBound = blocked ? lowBoundBlocked : lowBoundUnblocked;
        int highBound = blocked ? highBoundBlocked : highBoundUnblocked;
        SortedSet<Integer> range = new TreeSet<Integer>();
        for (int i = lowBound; i <= highBound; i++) {
            range.add(i);
        }
        return range;
    }

    private SortedSet<Integer> generatePredictionSet() {
        int absB = Math.abs(bChamber.getStatus() - 50);
        int absC = Math.abs(cChamber.getStatus() - 50);
        int prediction1 = clamp(-3 * mine.getStabilityDiff() + 125 - absB - absC);
        int prediction2 = clamp(3 * mine.getStabilityDiff() - 25 + absB + absC);
        SortedSet<Integer> predictions = new TreeSet<Integer>();
        predictions.add(prediction1);
        predictions.add(prediction2);
        return predictions;
    }

    private int updatePrediction(int prediction, boolean aBlocked, boolean aFailing) {
        int parity = aBlocked ? 1 : -1;
        int magnitude = aFailing ? 2 : 1;
        return clamp(prediction + parity * magnitude);
    }

    private void updatePredictions(SortedSet<Integer> newPredictions, boolean aFailing) {
        updates.add(new ChamberUpdate(mine.getAChamber().isBlocked(), aFailing));
        predictions = newPredictions;
        updateInGameString();
    }

    public void updateInGameString() {
        if (predictions.size() == 0) {
            inGameString = "(?)%";
            return;
        }
        boolean isRange = !isBetweenIncl(predictions.size(), 1, 2);
        inGameString = "(";
        int firstPrediction = predictions.first();
        if (isRange) {
            List<Integer> ranges = new ArrayList<Integer>();
            ranges.add(firstPrediction);
            Iterator<Integer> iterator = predictions.iterator();
            while (iterator.hasNext()) {
                int curPrediction = iterator.next();
                if (curPrediction == firstPrediction) {
                    continue;
                }
                if (iterator.hasNext()) {
                    int nextPrediction = iterator.next();
                    if (nextPrediction - curPrediction != 1) {
                        ranges.add(curPrediction);
                        ranges.add(nextPrediction);
                    }
                }
            }
            ranges.add(predictions.last());
            for (int i = 0; i < ranges.size(); i += 2) {
                inGameString += String.format("%d-%d", ranges.get(i), ranges.get(i + 1));
                if (i != ranges.size() - 2) {
                    inGameString += " | ";
                }
            }
        } else {
            inGameString += String.valueOf(firstPrediction);
            if (predictions.size() == 2) {
                inGameString += " | " + String.valueOf(predictions.last());
            }
        }
        inGameString += ")%";
    }

    private boolean isChamberFailing() {
        int bChange = Math.abs(bChamber.getStatusDiff());
        int cChange = Math.abs(cChamber.getStatusDiff());
        return (bChange == 2 || cChange == 2 || (bChange == 1 && !bChamber.isPrevFailing())
                || (bChange == 0 && cChange == 1 && !cChamber.isPrevFailing())
                || (bChamber.isMaxed() && cChamber.isMaxed() && mine.getStabilityDiff() <= -12));
    }

    @Override
    public SortedSet<Integer> getPredictions() {
        return predictions;
    }

    @Override
    public String getInGameString() {
        return inGameString;
    }
}
