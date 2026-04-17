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
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.ImageProxy;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.islboggle.CameraManager;
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
    private Button clearButton, deleteButton, submitButton;

    private CameraManager cameraManager;
    private MediaPipeHandler mediaPipe;
    private LandmarkProcessor landmarkProcessor;
    private ModelRunner modelRunner;
    private PredictionManager predictionManager;

    private int levelId;
    private long timeLimitMs;

    private final Handler ui = new Handler(Looper.getMainLooper());
    private volatile String lastStableLetter = "";
    private volatile long lastAppendMs = 0L;
    private static final long APPEND_COOLDOWN_MS = 1500L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        levelId = getIntent().getIntExtra("LEVEL_ID", 1);
        timeLimitMs = getIntent().getLongExtra("TIME_LIMIT", 60000L);

        viewModel = new ViewModelProvider(this).get(GameViewModel.class);

        previewView = findViewById(R.id.previewView);
        timerText = findViewById(R.id.timerText);
        scoreText = findViewById(R.id.scoreText);
        statusText = findViewById(R.id.statusText);
        predictionLetterText = findViewById(R.id.predictionLetterText);
        wordBuilderText = findViewById(R.id.wordBuilderText);
        boggleGrid = findViewById(R.id.boggleGrid);
        clearButton = findViewById(R.id.clearButton);
        deleteButton = findViewById(R.id.deleteButton);
        submitButton = findViewById(R.id.submitButton);

        clearButton.setOnClickListener(v -> {
            viewModel.clearWord();
            predictionManager.clearHistory();
            lastStableLetter = "";
        });

        deleteButton.setOnClickListener(v -> {
            viewModel.deleteLetter();
        });

        submitButton.setOnClickListener(v -> {
            viewModel.submitWord();
            lastStableLetter = "";
            predictionManager.clearHistory();
        });

        landmarkProcessor = new LandmarkProcessor();
        predictionManager = new PredictionManager();

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

        viewModel.startLevel(timeLimitMs);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void observeViewModel() {
        viewModel.getTimeRemaining().observe(this, millis -> {
            int seconds = (int) (millis / 1000) % 60;
            int minutes = (int) ((millis / (1000 * 60)) % 60);
            timerText.setText(String.format("%d:%02d", minutes, seconds));
        });

        viewModel.getCurrentWord().observe(this, word -> wordBuilderText.setText(word.isEmpty() ? "-" : word));
        viewModel.getScore().observe(this, score -> scoreText.setText("Score: " + score));
        viewModel.getStatusMessage().observe(this, msg -> statusText.setText(msg));

        viewModel.getGrid().observe(this, grid -> {
            if (grid != null) populateGrid(grid);
        });

        viewModel.getIsGameOver().observe(this, isOver -> {
            if (isOver) handleGameOver();
        });
    }

    private void populateGrid(char[][] gridValues) {
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
                tv.setBackgroundColor(Color.parseColor("#444444"));
                tv.setGravity(Gravity.CENTER);

                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                // We estimate a good size for typical screens. Using DP is better but this works for demo
                params.width = 160;
                params.height = 160;
                params.setMargins(8, 8, 8, 8);
                tv.setLayoutParams(params);

                boggleGrid.addView(tv);
            }
        }
    }

    private void handleGameOver() {
        if (cameraManager != null) cameraManager.shutdown();
        submitButton.setEnabled(false);
        clearButton.setEnabled(false);
        deleteButton.setEnabled(false);
        
        LevelRepository repo = new LevelRepository(this);
        repo.unlockNextLevel(levelId);

        Toast.makeText(this, "Time's up! Level " + levelId + " Completed. Score: " + viewModel.getFinalScore(), Toast.LENGTH_LONG).show();
        finish();
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
            float[] flat = landmarkProcessor.process(result);

            if (flat == null) {
                ui.post(() -> predictionLetterText.setText("-"));
                return;
            }

            float[] probs = modelRunner.run(flat);
            if (probs == null) return;

            PredictionManager.Prediction p = predictionManager.update(probs);

            ui.post(() -> {
                if (!p.letter.isEmpty()) {
                    predictionLetterText.setText(p.letter + " (" + String.format("%.2f", p.confidence) + ")");
                }
                if (p.stable && !p.letter.isEmpty()) {
                    long now = System.currentTimeMillis();
                    boolean cooldown = (now - lastAppendMs) < APPEND_COOLDOWN_MS;
                    if (!cooldown || !p.letter.equals(lastStableLetter)) {
                        Log.i(TAG, "[ML_PIPELINE] Final Accepted Prediction: " + p.letter);
                        viewModel.appendLetter(p.letter);
                        lastStableLetter = p.letter;
                        lastAppendMs = now;
                        predictionManager.clearHistory();
                    }
                }
            });
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
