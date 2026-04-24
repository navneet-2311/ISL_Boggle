package com.example.islboggle.ui.game;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.ImageProxy;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.islboggle.CameraManager;
import com.example.islboggle.FrameBuffer;
import com.example.islboggle.Labels;
import com.example.islboggle.LandmarkProcessor;
import com.example.islboggle.MediaPipeHandler;
import com.example.islboggle.ModelRunner;
import com.example.islboggle.PredictionManager;
import com.example.islboggle.R;
import com.example.islboggle.data.LevelRepository;
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult;

import java.io.IOException;

public class GameActivity extends AppCompatActivity {

    private static final String TAG = "GameActivity";

    private GameViewModel viewModel;
    private PreviewView previewView;
    private TextView timerText, scoreText, statusText, predictionLetterText, wordBuilderText;
    private GridLayout boggleGrid;
    private Button recordButton;

    private CameraManager cameraManager;
    private MediaPipeHandler mediaPipe;
    private LandmarkProcessor landmarkProcessor;
    private ModelRunner modelRunner;
    private PredictionManager predictionManager;
    private FrameBuffer frameBuffer;

    private int levelId;
    private boolean isRecording = false;
    private boolean handInFrame = false;
    private String lastDebugPrediction = "";
    private final Handler ui = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        levelId = getIntent().getIntExtra("LEVEL_ID", 1);

        viewModel = new ViewModelProvider(this).get(GameViewModel.class);

        previewView = findViewById(R.id.previewView);
        timerText = findViewById(R.id.timerText);
        scoreText = findViewById(R.id.scoreText);
        statusText = findViewById(R.id.statusText);
        predictionLetterText = findViewById(R.id.predictionLetterText);
        wordBuilderText = findViewById(R.id.wordBuilderText);
        boggleGrid = findViewById(R.id.boggleGrid);
        recordButton = findViewById(R.id.recordButton);

        recordButton.setOnClickListener(v -> startRecordingSequence());

        landmarkProcessor = new LandmarkProcessor();
        predictionManager = new PredictionManager();
        frameBuffer = new FrameBuffer();

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

        observeViewModel();
        viewModel.startLevel(levelId);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void startRecordingSequence() {
        if (isRecording) return;
        
        isRecording = true;
        lastDebugPrediction = "";
        frameBuffer.clear();
        recordButton.setEnabled(false);
        recordButton.setText("RECORDING...");
        recordButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.GRAY));
    }

    private void stopRecordingSequence() {
        isRecording = false;
        recordButton.setEnabled(true);
        recordButton.setText("RECORD GESTURE");
        recordButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#D32F2F")));
        
        processRecordedBuffer();
    }

    private void processRecordedBuffer() {
        float[][][][] input = frameBuffer.getFlattenedInput();
        float[] probs = modelRunner.run(input);

        if (probs != null && probs.length > 0) {
            int argmax = 0;
            float maxProb = probs[0];
            for (int i = 1; i < probs.length; i++) {
                if (probs[i] > maxProb) {
                    maxProb = probs[i];
                    argmax = i;
                }
            }

            String word = Labels.forIndex(argmax);
            if (!word.isEmpty()) {
                lastDebugPrediction = "Debug: " + word + " (" + String.format("%.2f", maxProb) + ")";
                ui.post(() -> predictionLetterText.setText(lastDebugPrediction));
                viewModel.onWordPredicted(word, maxProb);
            } else {
                lastDebugPrediction = "Unknown Index: " + argmax;
                ui.post(() -> predictionLetterText.setText(lastDebugPrediction));
            }
        } else {
            lastDebugPrediction = "Error: No model output";
            ui.post(() -> predictionLetterText.setText(lastDebugPrediction));
            Log.e(TAG, "Inference failed: model returned null or empty");
        }
    }

    private void observeViewModel() {
        viewModel.getTimeRemaining().observe(this, millis -> {
            int seconds = (int) (millis / 1000) % 60;
            int minutes = (int) ((millis / (1000 * 60)) % 60);
            timerText.setText(String.format("%d:%02d", minutes, seconds));
        });

        viewModel.getLastPredictedWord().observe(this, word -> wordBuilderText.setText("Last: " + word));
        viewModel.getScore().observe(this, score -> scoreText.setText("Score: " + score));
        viewModel.getStatusMessage().observe(this, msg -> statusText.setText(msg));

        viewModel.getGrid().observe(this, grid -> updateGridDisplay());
        viewModel.getHighlighted().observe(this, highlighted -> updateGridDisplay());

        viewModel.getIsGameOver().observe(this, isOver -> {
            if (isOver) handleGameOver("Time's Up!");
        });

        viewModel.getIsLevelComplete().observe(this, complete -> {
            if (complete) handleGameOver("Level Complete!");
        });
    }

    private void updateGridDisplay() {
        char[][] gridValues = viewModel.getGrid().getValue();
        boolean[][] highlights = viewModel.getHighlighted().getValue();
        if (gridValues == null || highlights == null) return;

        boggleGrid.removeAllViews();
        int size = gridValues.length;
        boggleGrid.setRowCount(size);
        boggleGrid.setColumnCount(size);

        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                TextView tv = new TextView(this);
                tv.setText(String.valueOf(gridValues[r][c]));
                tv.setTextSize(32f);
                tv.setTypeface(null, Typeface.BOLD);
                tv.setTextColor(Color.WHITE);
                
                if (highlights[r][c]) {
                    tv.setBackgroundColor(Color.parseColor("#2E7D32"));
                } else {
                    tv.setBackgroundColor(Color.parseColor("#444444"));
                }
                
                tv.setGravity(Gravity.CENTER);
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = 160;
                params.height = 160;
                params.setMargins(8, 8, 8, 8);
                tv.setLayoutParams(params);
                boggleGrid.addView(tv);
            }
        }
    }

    private void handleGameOver(String title) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage("Your final score: " + viewModel.getFinalScore())
                .setCancelable(false)
                .setPositiveButton("Retry", (dialog, which) -> {
                    viewModel.retry();
                    frameBuffer.clear();
                    lastDebugPrediction = "";
                })
                .setNegativeButton("Back to Levels", (dialog, which) -> {
                    LevelRepository repo = new LevelRepository(this);
                    repo.unlockNextLevel(viewModel.getCurrentLevelId());
                    finish();
                })
                .show();
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

    private void onFrame(ImageProxy image) {
        try {
            if (mediaPipe == null || modelRunner == null) return;
            if (Boolean.TRUE.equals(viewModel.getIsGameOver().getValue())) return;

            HandLandmarkerResult result = mediaPipe.detect(image);
            handInFrame = (result != null && !result.landmarks().isEmpty());
            float[][] frameLandmarks = landmarkProcessor.process(result);
            
            if (isRecording) {
                frameBuffer.addFrame(frameLandmarks);
                int count = frameBuffer.getFrameCount();
                ui.post(() -> {
                    String status = handInFrame ? " (Hand OK)" : " (No Hand!)";
                    predictionLetterText.setText("Recording" + status + ": " + count + "/" + FrameBuffer.BUFFER_SIZE);
                    if (count >= FrameBuffer.BUFFER_SIZE) {
                        stopRecordingSequence();
                    }
                });
            } else {
                ui.post(() -> {
                    if (!lastDebugPrediction.isEmpty()) {
                        predictionLetterText.setText(lastDebugPrediction);
                    } else {
                        predictionLetterText.setText(handInFrame ? "Hand Detected - Ready" : "Tap Record to Start");
                    }
                });
            }
        } finally {
            image.close();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraManager != null) cameraManager.shutdown();
        if (mediaPipe != null) mediaPipe.close();
        if (modelRunner != null) modelRunner.close();
    }
}
