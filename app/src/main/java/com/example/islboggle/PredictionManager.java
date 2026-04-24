package com.example.islboggle;

import android.util.Log;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Applies confidence threshold + majority voting over the last N predictions
 * to produce a stable word.
 */
public class PredictionManager {

    private static final String TAG = "PredictionManager";
    public static final float DEFAULT_THRESHOLD = 0.7f;
    public static final int   DEFAULT_WINDOW    = 5;
    public static final int   DEFAULT_MAJORITY  = 3; // out of 5

    private final float threshold;
    private final int windowSize;
    private final int majorityCount;
    private final Deque<Integer> history = new ArrayDeque<>();

    public PredictionManager() {
        this(DEFAULT_THRESHOLD, DEFAULT_WINDOW, DEFAULT_MAJORITY);
    }

    public PredictionManager(float threshold, int windowSize, int majorityCount) {
        this.threshold = threshold;
        this.windowSize = windowSize;
        this.majorityCount = majorityCount;
    }

    public static class Prediction {
        public final int    classIndex;     // -1 if none
        public final float  confidence;
        public final String word;           // "" if none
        public final boolean stable;        // true when majority criterion met
        public Prediction(int classIndex, float confidence, String word, boolean stable) {
            this.classIndex = classIndex;
            this.confidence = confidence;
            this.word = word;
            this.stable = stable;
        }
        public static Prediction none() { return new Prediction(-1, 0f, "", false); }
    }

    /**
     * Feed model probabilities; returns latest prediction info (and whether stable).
     */
    public Prediction update(float[] probs) {
        if (probs == null || probs.length == 0) {
            return Prediction.none();
        }
        int argmax = 0;
        float best = probs[0];
        for (int i = 1; i < probs.length; i++) {
            if (probs[i] > best) { best = probs[i]; argmax = i; }
        }

        if (best < threshold) {
            return new Prediction(argmax, best, Labels.forIndex(argmax), false);
        }

        // Push to history
        history.addLast(argmax);
        while (history.size() > windowSize) history.removeFirst();

        // Majority vote
        Map<Integer, Integer> counts = new HashMap<>();
        int topClass = -1;
        int topCount = 0;
        for (int idx : history) {
            int c = counts.getOrDefault(idx, 0) + 1;
            counts.put(idx, c);
            if (c > topCount) { topCount = c; topClass = idx; }
        }

        boolean stable = history.size() >= windowSize && topCount >= majorityCount;
        
        return new Prediction(argmax, best, Labels.forIndex(argmax), stable);
    }

    public void clearHistory() {
        history.clear();
    }
}
