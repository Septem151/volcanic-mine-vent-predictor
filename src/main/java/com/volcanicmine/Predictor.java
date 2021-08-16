package com.volcanicmine;

import static com.volcanicmine.NumUtil.clamp;
import java.util.ArrayList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Predictor {
    private final Mine mine;

    @Getter
    private Prediction prediction;

    private final ArrayList<Prediction> allPredictions;

    public Predictor(Mine mine) {
        this.mine = mine;
        allPredictions = new ArrayList<Prediction>();
        prediction = null;
    }

    public void predictOnVentUpdate() {
        if (prediction == null) {
            setInitialPrediction();
        } else {
            boolean aFailing = isAFailing();
            log.info("On vent update: A is failing: " + aFailing);
            int parity = mine.getAChamber().isBlocked() ? 1 : -1;
            int magnitude = aFailing ? 2 : 1;
            int newLow = clamp(prediction.getLowGuess() + (parity * magnitude));
            int newHigh = clamp(prediction.getHighGuess() + (parity * magnitude));
            boolean lowCorrect = prediction.isLowCorrect()
                    && ((aFailing && newLow <= 40 || aFailing && newLow >= 60)
                            || (!aFailing && newLow >= 40 || !aFailing && newLow <= 60));
            boolean highCorrect = prediction.isHighCorrect()
                    && ((aFailing && newHigh <= 40 || aFailing && newHigh >= 60)
                            || (!aFailing && newHigh >= 40 && !aFailing && newHigh <= 60));
            if (mine.getStability() == 100 && !lowCorrect && !highCorrect) {
                lowCorrect = highCorrect = true;
                newLow = prediction.getLowGuess();
                newHigh = prediction.getHighGuess();
            }
            prediction = new Prediction(newLow, lowCorrect, newHigh, highCorrect, aFailing);
            log.info("New Low: " + newLow + ", New High: " + newHigh + ", Low Correct: "
                    + lowCorrect + ", High Correct: " + highCorrect);
        }
        allPredictions.add(prediction);
    }

    public void predictOnStabilityUpdate() {
        if (mine.getStability() == 100) {
            return;
        }
        int stabilityChange = mine.getStability() - mine.getPrevStability();
        log.info("Running in stability update");
        int absB = Math.abs(mine.getBChamber().getStatus() - 50);
        int absC = Math.abs(mine.getCChamber().getStatus() - 50);
        int negAbsPrediction = clamp(-3 * stabilityChange + 125 - absB - absC);
        int posAbsPrediction = clamp(3 * stabilityChange - 25 + absB + absC);
        int lowGuess = Math.min(negAbsPrediction, posAbsPrediction);
        int highGuess = Math.max(negAbsPrediction, posAbsPrediction);
        boolean lowCorrect = true;
        boolean highCorrect = true;
        boolean aFailing = isAFailing();
        int parity = mine.getAChamber().isBlocked() ? 1 : -1;
        int tempLowGuess = lowGuess;
        int tempHighGuess = highGuess;
        if (allPredictions.size() > 0) {
            allPredictions.remove(allPredictions.size() - 1);
        }
        if (lowCorrect && highCorrect) {
            for (int i = allPredictions.size() - 1; i >= 0; i--) {
                Prediction oldPrediction = allPredictions.get(i);
                int magnitude = oldPrediction.isAFailing() ? 2 : 1;
                tempLowGuess -= parity * magnitude;
                tempHighGuess -= parity * magnitude;
                lowCorrect = tempLowGuess >= 0;
                highCorrect = tempHighGuess <= 100;
                if (!lowCorrect || !highCorrect) {
                    break;
                }
            }
        }
        prediction = new Prediction(lowGuess, lowCorrect, highGuess, highCorrect, aFailing);
        log.info("New stability prediction: " + lowGuess + ", " + highGuess + ", " + lowCorrect
                + ", " + highCorrect);
        allPredictions.add(prediction);
    }

    public void setInitialPrediction() {
        boolean aFailing = isAFailing();
        if (aFailing) {
            prediction = new Prediction(0, true, 100, true, aFailing);
        } else {
            prediction = new Prediction(50, true, 50, true, aFailing);
        }
    }

    public boolean isAFailing() {
        int bStatus = mine.getBChamber().getStatus();
        int cStatus = mine.getCChamber().getStatus();
        int bChange = Math.abs(bStatus - mine.getBChamber().getPrevStatus());
        int cChange = Math.abs(cStatus - mine.getCChamber().getPrevStatus());
        int stabilityChange = mine.getStability() - mine.getPrevStability();
        boolean bFailing = mine.getBChamber().isFailing();
        boolean cFailing = mine.getCChamber().isFailing();
        boolean bMaxed = bChange == 0 && (bStatus == 0 || bStatus == 100);
        boolean cMaxed = cChange == 0 && (cStatus == 0 || cStatus == 100);
        return bChange == 2 || cChange == 2 || (bChange == 1 && !bFailing)
                || (bChange == 0 && cChange == 1 && !cFailing)
                || (bMaxed && cMaxed && stabilityChange <= -12);
    }
}
