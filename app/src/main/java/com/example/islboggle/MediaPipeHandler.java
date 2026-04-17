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
                        .setNumHands(1)
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
        Bitmap bitmap = imageProxyToBitmap(imageProxy);
        if (bitmap == null) return null;
        int rotation = imageProxy.getImageInfo().getRotationDegrees();
        if (rotation != 0) {
            Matrix m = new Matrix();
            m.postRotate(rotation);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
        }
        MPImage mpImage = new BitmapImageBuilder(bitmap).build();
        try {
            return handLandmarker.detect(mpImage);
        } catch (Exception e) {
            Log.e(TAG, "MediaPipe detect failed", e);
            return null;
        }
    }

    private Bitmap imageProxyToBitmap(ImageProxy imageProxy) {
        Image image = imageProxy.getImage();
        if (image == null) return null;

        // YUV_420_888 -> NV21 -> JPEG -> Bitmap
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
        yuvImage.compressToJpeg(new android.graphics.Rect(0, 0, image.getWidth(), image.getHeight()), 85, out);
        byte[] jpegBytes = out.toByteArray();
        return android.graphics.BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.length);
    }

    public void close() {
        if (handLandmarker != null) handLandmarker.close();
    }
}
