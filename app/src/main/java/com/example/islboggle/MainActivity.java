package com.example.islboggle;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.ImageProxy;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult;

import java.io.IOException;

/**
 * Wires the pipeline:
 * Camera → MediaPipe → Landmarks → TFLite → Stabilize → Game → UI
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private PreviewView previewView;
    private TextView    letterText, wordText, scoreText, statusText;
    private Button      resetButton, commitButton;

    private CameraManager       cameraManager;
    private MediaPipeHandler    mediaPipe;
    private LandmarkProcessor   landmarkProcessor;
    private ModelRunner         modelRunner;
    private PredictionManager   predictionManager;
    private GameEngine          gameEngine;

    private final Handler ui = new Handler(Looper.getMainLooper());
    private volatile String lastStableLetter = "";
    private volatile long   lastAppendMs     = 0L;
    private static final long APPEND_COOLDOWN_MS = 1500L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewView   = findViewById(R.id.previewView);
        letterText    = findViewById(R.id.letterText);
        wordText      = findViewById(R.id.wordText);
        scoreText     = findViewById(R.id.scoreText);
        statusText    = findViewById(R.id.statusText);
        resetButton   = findViewById(R.id.resetButton);
        commitButton  = findViewById(R.id.commitButton);

        landmarkProcessor = new LandmarkProcessor();
        predictionManager = new PredictionManager();
        gameEngine        = new GameEngine(this);

        try {
            modelRunner = new ModelRunner(this);
        } catch (IOException e) {
            Log.e(TAG, "Model load failed", e);
            Toast.makeText(this, "Failed to load model.tflite", Toast.LENGTH_LONG).show();
        }

        try {
            mediaPipe = new MediaPipeHandler(this);
        } catch (Exception e) {
            Log.e(TAG, "MediaPipe init failed", e);
            Toast.makeText(this, "MediaPipe init failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        resetButton.setOnClickListener(v -> {
            gameEngine.resetWord();
            predictionManager.clearHistory();
            lastStableLetter = "";
            refreshUi();
        });

        commitButton.setOnClickListener(v -> {
            int pts = gameEngine.tryCommitWord();
            if (pts > 0) {
                Toast.makeText(this, "+" + pts + " points!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Not in dictionary", Toast.LENGTH_SHORT).show();
            }
            refreshUi();
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA);
        }

        statusText.setText("Dictionary: " + gameEngine.getDictionarySize() + " words");
        refreshUi();
    }

    private final androidx.activity.result.ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) startCamera();
                else Toast.makeText(this, "Camera permission required", Toast.LENGTH_LONG).show();
            });

    private void startCamera() {
        cameraManager = new CameraManager(this, this, previewView);
        cameraManager.start(this::onFrame);
    }

    /** Runs on CameraX analysis executor (background thread). */
    private void onFrame(ImageProxy image) {
        try {
            if (mediaPipe == null || modelRunner == null) return;

            HandLandmarkerResult result = mediaPipe.detect(image);
            float[] flat = landmarkProcessor.process(result);

            if (flat == null) {
                // No hand: skip inference for this frame.
                ui.post(() -> letterText.setText("-"));
                return;
            }

            float[] probs = modelRunner.run(flat);
            if (probs == null) return;

            PredictionManager.Prediction p = predictionManager.update(probs);

            ui.post(() -> {
                if (!p.letter.isEmpty()) {
                    letterText.setText(p.letter + "  (" + String.format("%.2f", p.confidence) + ")");
                }
                if (p.stable && !p.letter.isEmpty()) {
                    long now = System.currentTimeMillis();
                    boolean cooldown = (now - lastAppendMs) < APPEND_COOLDOWN_MS;
                    if (!cooldown || !p.letter.equals(lastStableLetter)) {
                        gameEngine.appendLetter(p.letter);
                        lastStableLetter = p.letter;
                        lastAppendMs = now;
                        predictionManager.clearHistory();
                        // Auto-validate on each new letter
                        int pts = gameEngine.tryCommitWord();
                        if (pts > 0) {
                            Toast.makeText(this, "+" + pts + " points!", Toast.LENGTH_SHORT).show();
                        }
                        refreshUi();
                    }
                }
            });
        } finally {
            image.close();
        }
    }

    private void refreshUi() {
        wordText.setText("Word: " + gameEngine.getCurrentWord());
        scoreText.setText("Score: " + gameEngine.getScore());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraManager != null) cameraManager.shutdown();
        if (mediaPipe != null) mediaPipe.close();
        if (modelRunner != null) modelRunner.close();
    }
}
