package com.example.islboggle;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageProxy;

import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker;
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult;

/**
 * Wraps MediaPipe Hand Landmarker. Runs in IMAGE mode synchronously.
 */
public class MediaPipeHandler {

    private static final String TAG = "MediaPipeHandler";
    private static final String MODEL_ASSET = "hand_landmarker.task";

    private final HandLandmarker handLandmarker;

    public MediaPipeHandler(Context context) {
        BaseOptions baseOptions = BaseOptions.builder()
                .setModelAssetPath(MODEL_ASSET)
                .build();

        HandLandmarker.HandLandmarkerOptions options =
                HandLandmarker.HandLandmarkerOptions.builder()
                        .setBaseOptions(baseOptions)
                        .setRunningMode(RunningMode.IMAGE)
                        .setNumHands(2)
                        .setMinHandDetectionConfidence(0.3f) // Lowered slightly to improve detection
                        .setMinHandPresenceConfidence(0.3f)
                        .setMinTrackingConfidence(0.3f)
                        .build();

        handLandmarker = HandLandmarker.createFromOptions(context, options);
        Log.i(TAG, "Hand Landmarker initialized");
    }

    /**
     * Detect landmarks from a CameraX ImageProxy.
     */
    public HandLandmarkerResult detect(@NonNull ImageProxy imageProxy) {
        try {
            // Use CameraX's built-in conversion which handles rotation and YUV format correctly.
            // This ensures MediaPipe sees the image in the correct upright orientation.
            Bitmap bitmap = imageProxy.toBitmap();
            if (bitmap == null) return null;

            MPImage mpImage = new BitmapImageBuilder(bitmap).build();
            return handLandmarker.detect(mpImage);
        } catch (Exception e) {
            Log.e(TAG, "MediaPipe detect failed", e);
            return null;
        }
    }

    public void close() {
        if (handLandmarker != null) handLandmarker.close();
    }
}
