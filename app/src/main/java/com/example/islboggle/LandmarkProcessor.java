package com.example.islboggle;

import android.util.Log;

import com.google.mediapipe.tasks.components.containers.Category;
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult;

import java.util.List;

/**
 * Converts a MediaPipe HandLandmarkerResult into a flat float[84] matching
 * the reference implementation precisely (Case A for 1 hand, Case B for 2 hands).
 */
public class LandmarkProcessor {

    private static final String TAG = "LandmarkProcessor";
    public static final int NUM_LANDMARKS = 21;
    public static final int FLAT_SIZE = 84; 

    /**
     * @param result MediaPipe result (may be null)
     * @return float[84] flat array mapped exactly to the required vector space
     */
    public float[] process(HandLandmarkerResult result) {
        if (result == null) return null;
        List<List<NormalizedLandmark>> landmarksList = result.landmarks();
        List<List<Category>> handednessesList = result.handednesses();
        
        // Skip frame if no hands or >2 hands
        if (landmarksList == null || landmarksList.isEmpty() || landmarksList.size() > 2) {
            return null;
        }

        // Implicitly initializes to 0.0f
        float[] vector = new float[FLAT_SIZE];
        float maxValue = 0f;

        if (landmarksList.size() == 1) {
            // CASE A: ONE HAND DETECTED
            List<NormalizedLandmark> hand = landmarksList.get(0);
            if (hand == null || hand.size() < NUM_LANDMARKS) return null;

            float baseX = hand.get(0).x();
            float baseY = hand.get(0).y();

            for (int i = 0; i < NUM_LANDMARKS; i++) {
                NormalizedLandmark lm = hand.get(i);
                
                float yRel = lm.y() - baseY;
                float xRel = lm.x() - baseX;
                
                vector[42 + (i * 2)] = yRel;
                vector[42 + (i * 2) + 1] = xRel;
                
                if (Math.abs(yRel) > maxValue) maxValue = Math.abs(yRel);
                if (Math.abs(xRel) > maxValue) maxValue = Math.abs(xRel);
            }
        } 
        else if (landmarksList.size() == 2) {
            // CASE B: TWO HANDS DETECTED
            List<NormalizedLandmark> leftHand = null;
            List<NormalizedLandmark> rightHand = null;

            for (int h = 0; h < 2; h++) {
                if (handednessesList.get(h) != null && !handednessesList.get(h).isEmpty()) {
                    String category = handednessesList.get(h).get(0).categoryName();
                    if ("Left".equalsIgnoreCase(category)) {
                        leftHand = landmarksList.get(h);
                    } else if ("Right".equalsIgnoreCase(category)) {
                        rightHand = landmarksList.get(h);
                    }
                }
            }
            
            // Failsafe if categories weren't explicitly "Left"/"Right"
            if (leftHand == null || rightHand == null) {
                 leftHand = landmarksList.get(0);
                 rightHand = landmarksList.get(1);
            }

            if (leftHand.size() < NUM_LANDMARKS || rightHand.size() < NUM_LANDMARKS) return null;

            float leftWristX = leftHand.get(0).x();
            float leftWristY = leftHand.get(0).y();
            float rightWristX = rightHand.get(0).x();
            float rightWristY = rightHand.get(0).y();

            float baseX = (leftWristX + rightWristX) / 2f;
            float baseY = (leftWristY + rightWristY) / 2f;

            // LEFT hand constraints
            for (int i = 0; i < NUM_LANDMARKS; i++) {
                NormalizedLandmark lm = leftHand.get(i);
                float yRel = lm.y() - baseY;
                float xRel = lm.x() - baseX;
                
                vector[i * 2] = yRel;
                vector[(i * 2) + 1] = xRel;
                
                if (Math.abs(yRel) > maxValue) maxValue = Math.abs(yRel);
                if (Math.abs(xRel) > maxValue) maxValue = Math.abs(xRel);
            }

            // RIGHT hand constraints
            for (int i = 0; i < NUM_LANDMARKS; i++) {
                NormalizedLandmark lm = rightHand.get(i);
                float yRel = lm.y() - baseY;
                float xRel = lm.x() - baseX;
                
                vector[42 + (i * 2)] = yRel;
                vector[42 + (i * 2) + 1] = xRel;
                
                if (Math.abs(yRel) > maxValue) maxValue = Math.abs(yRel);
                if (Math.abs(xRel) > maxValue) maxValue = Math.abs(xRel);
            }
        }

        // Normalization applied spanning both scenarios natively
        if (maxValue != 0f) {
            for (int i = 0; i < FLAT_SIZE; i++) {
                vector[i] /= maxValue;
            }
        }

        // Required Debug Logging
        Log.d(TAG, "Hands Detected: " + landmarksList.size());
        Log.d(TAG, "Vector length: " + vector.length);
        StringBuilder sb = new StringBuilder("Vector [");
        for (int i = 0; i < 6; i++) {
            if (i > 0) sb.append(", ");
            sb.append(String.format("%.4f", vector[i]));
            if (i == 5) sb.append(" ...]");
        }
        Log.d(TAG, sb.toString());
        
        return vector;
    }
}
