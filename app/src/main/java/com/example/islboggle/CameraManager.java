package com.example.islboggle;

import android.content.Context;
import android.util.Log;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * CameraX setup. Binds a Preview to PreviewView and an ImageAnalysis use case
 * that delivers frames to a background executor.
 */
public class CameraManager {

    private static final String TAG = "CameraManager";

    public interface FrameAnalyzer {
        void analyze(@NonNull ImageProxy image);
    }

    private final Context context;
    private final LifecycleOwner lifecycleOwner;
    private final PreviewView previewView;
    private final ExecutorService analysisExecutor;

    public CameraManager(Context context, LifecycleOwner owner, PreviewView previewView) {
        this.context = context;
        this.lifecycleOwner = owner;
        this.previewView = previewView;
        this.analysisExecutor = Executors.newSingleThreadExecutor();
    }

    public void start(FrameAnalyzer analyzer) {
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(context);
        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();
                bind(provider, analyzer);
            } catch (Exception e) {
                Log.e(TAG, "Camera init failed", e);
            }
        }, ContextCompat.getMainExecutor(context));
    }

    private void bind(ProcessCameraProvider provider, FrameAnalyzer analyzer) {
        Preview preview = new Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        ImageAnalysis analysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(480, 640))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        analysis.setAnalyzer(analysisExecutor, analyzer::analyze);

        CameraSelector selector = CameraSelector.DEFAULT_FRONT_CAMERA;

        provider.unbindAll();
        provider.bindToLifecycle(lifecycleOwner, selector, preview, analysis);
        Log.i(TAG, "Camera bound");
    }

    public void shutdown() {
        analysisExecutor.shutdown();
    }
}
