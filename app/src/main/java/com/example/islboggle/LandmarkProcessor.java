package com.example.islboggle;

import com.google.mediapipe.tasks.components.containers.Category;
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult;

import java.util.List;

/**
 * Converts MediaPipe HandLandmarkerResult into a [42, 3] array.
 * 21 landmarks for Left Hand + 21 landmarks for Right Hand.
 * If a hand is missing, it is zero-filled.
 */
public class LandmarkProcessor {

    public static final int NUM_LANDMARKS = 21;
    public static final int TOTAL_LANDMARKS = 42; // 21 Left + 21 Right

    /**
     * @param result MediaPipe result
     * @return float[42][3] array
     */
    public float[][] process(HandLandmarkerResult result) {
        float[][] frame = new float[TOTAL_LANDMARKS][3];

        if (result == null || result.landmarks().isEmpty()) {
            return frame; // Return all zeros
        }

        List<List<NormalizedLandmark>> landmarksList = result.landmarks();
        List<List<Category>> handednessesList = result.handednesses();

        for (int i = 0; i < landmarksList.size(); i++) {
            List<NormalizedLandmark> landmarks = landmarksList.get(i);
            String label = handednessesList.get(i).get(0).categoryName();
            
            // Map to the correct 21-point block
            int offset = label.equalsIgnoreCase("Left") ? 0 : 21;
            
            for (int j = 0; j < NUM_LANDMARKS && j < landmarks.size(); j++) {
                NormalizedLandmark lm = landmarks.get(j);
                
                // NO ROTATION: Use raw coordinates directly
                frame[offset + j][0] = lm.x();
                frame[offset + j][1] = lm.y();
                frame[offset + j][2] = lm.z();
            }
        }

        return frame;
    }
}
