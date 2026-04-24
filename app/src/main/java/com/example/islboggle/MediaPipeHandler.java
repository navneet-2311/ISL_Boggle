package com.example.islboggle;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.Image;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageProxy;

import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker;
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

/**
 * Wraps MediaPipe Hand Landmarker. Runs in IMAGE mode synchronously
 * because we already drive it from a background analyzer thread.
 */
public class MediaPipeHandler {

    private static final String TAG = "MediaPipeHandler";
    // Bundled MediaPipe asset name. Place hand_landmarker.task in assets/.
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
                        .setMinHandDetectionConfidence(0.5f)
                        .setMinHandPresenceConfidence(0.5f)
                        .setMinTrackingConfidence(0.5f)
                        .build();

        handLandmarker = HandLandmarker.createFromOptions(context, options);
        Log.i(TAG, "Hand Landmarker initialized");
    }

    /**
     * Detect landmarks from a CameraX ImageProxy.
     * Caller is responsible for closing the ImageProxy.
     */
    public HandLandmarkerResult detect(@NonNull ImageProxy imageProxy) {
        // Optimization: Use MediaPipe's BitmapImageBuilder directly from the bitmap.
        // We'll also reuse objects if possible, but first let's fix the heavy conversion.
        Bitmap bitmap = imageProxyToBitmap(imageProxy);
        if (bitmap == null) return null;
        
        // Note: Rotation is handled in LandmarkProcessor, so we can potentially skip 
        // the heavy Bitmap rotation here to save CPU cycles.
        MPImage mpImage = new BitmapImageBuilder(bitmap).build();
        try {
            return handLandmarker.detect(mpImage);
        } catch (Exception e) {
            Log.e(TAG, "MediaPipe detect failed", e);
            return null;
        }
    }

    private Bitmap imageProxyToBitmap(ImageProxy imageProxy) {
        // Faster YUV to Bitmap conversion can be done via CameraX's internal utils 
        // or a more direct buffer access, but for now let's optimize the existing one.
        Image image = imageProxy.getImage();
        if (image == null) return null;

        // Using a simpler approach if possible, but YUV_420_888 to Bitmap is notoriously 
        // slow without specialized libraries. Let's try to reduce object creation.
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        android.graphics.YuvImage yuvImage = new android.graphics.YuvImage(
                nv21, android.graphics.ImageFormat.NV21,
                image.getWidth(), image.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // Use a lower quality for speed if needed, but 85 is usually okay.
        yuvImage.compressToJpeg(new android.graphics.Rect(0, 0, image.getWidth(), image.getHeight()), 80, out);
        byte[] jpegBytes = out.toByteArray();
        return android.graphics.BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.length);
    }

    public void close() {
        if (handLandmarker != null) handLandmarker.close();
    }
}
