package com.example.islboggle;

import android.util.Log;

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult;

import java.util.List;

/**
 * Converts a MediaPipe HandLandmarkerResult into a flat float[42]
 * containing only (x, y) for the 21 hand landmarks (z ignored).
 */
public class LandmarkProcessor {

    private static final String TAG = "LandmarkProcessor";
    public static final int NUM_LANDMARKS = 21;
    public static final int FLAT_SIZE = NUM_LANDMARKS * 2; // 42

    /**
     * @param result MediaPipe result (may be null)
     * @return float[42] flat array, or null if no hand detected
     */
    public float[] process(HandLandmarkerResult result) {
        if (result == null) return null;
        List<List<NormalizedLandmark>> landmarksList = result.landmarks();
        if (landmarksList == null || landmarksList.isEmpty()) return null;

        List<NormalizedLandmark> hand = landmarksList.get(0);
        if (hand == null || hand.size() < NUM_LANDMARKS) return null;

        float[] flat = new float[FLAT_SIZE];
        for (int i = 0; i < NUM_LANDMARKS; i++) {
            NormalizedLandmark lm = hand.get(i);
            flat[i * 2]     = lm.x();
            flat[i * 2 + 1] = lm.y();
            // z intentionally ignored
        }

        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            StringBuilder sb = new StringBuilder("flat42=[");
            for (int i = 0; i < flat.length; i++) {
                if (i > 0) sb.append(',');
                sb.append(flat[i]);
            }
            sb.append(']');
            Log.v(TAG, sb.toString());
        }
        return flat;
    }
}
