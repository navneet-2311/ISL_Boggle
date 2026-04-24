package com.example.islboggle.ui.game;

import android.app.Application;
import android.os.CountDownTimer;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.islboggle.GameEngine;
import com.example.islboggle.data.Level;
import com.example.islboggle.data.LevelRepository;

public class GameViewModel extends AndroidViewModel {
    private final GameEngine gameEngine;
    private final LevelRepository levelRepository;
    
    private final MutableLiveData<Integer> score = new MutableLiveData<>(0);
    private final MutableLiveData<char[][]> grid = new MutableLiveData<>();
    private final MutableLiveData<boolean[][]> highlighted = new MutableLiveData<>();
    private final MutableLiveData<Long> timeRemaining = new MutableLiveData<>(0L);
    private final MutableLiveData<Boolean> isGameOver = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isLevelComplete = new MutableLiveData<>(false);
    private final MutableLiveData<String> statusMessage = new MutableLiveData<>("");
    private final MutableLiveData<String> lastPredictedWord = new MutableLiveData<>("-");

    private CountDownTimer timer;
    private Level currentLevel;

    public GameViewModel(@NonNull Application application) {
        super(application);
        gameEngine = new GameEngine();
        levelRepository = new LevelRepository(application);
    }

    public void startLevel(int levelId) {
        this.currentLevel = levelRepository.getLevel(levelId);
        if (currentLevel == null) return;

        if (timer != null) timer.cancel();
        
        isGameOver.setValue(false);
        isLevelComplete.setValue(false);
        gameEngine.startLevel(currentLevel);
        
        grid.setValue(gameEngine.getGrid());
        highlighted.setValue(gameEngine.getHighlighted());
        score.setValue(0);
        statusMessage.setValue("Find 3 words!");
        lastPredictedWord.setValue("-");
        
        startTimer(currentLevel.timeLimitMs);
    }

    private void startTimer(long duration) {
        timer = new CountDownTimer(duration, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeRemaining.setValue(millisUntilFinished);
            }

            @Override
            public void onFinish() {
                timeRemaining.setValue(0L);
                isGameOver.setValue(true);
            }
        }.start();
    }

    public void onWordPredicted(String word, float confidence) {
        if (Boolean.TRUE.equals(isGameOver.getValue()) || Boolean.TRUE.equals(isLevelComplete.getValue())) return;
        if (confidence < 0.7f) return;

        lastPredictedWord.setValue(word + " (" + String.format("%.2f", confidence) + ")");
        
        int pts = gameEngine.tryMatchWord(word);
        if (pts > 0) {
            score.setValue(gameEngine.getScore());
            statusMessage.setValue("Matched: " + word + "!");
            highlighted.setValue(gameEngine.getHighlighted());
            
            if (gameEngine.isLevelComplete()) {
                if (timer != null) timer.cancel();
                isLevelComplete.setValue(true);
                statusMessage.setValue("Level Complete!");
            }
        }
    }

    public void retry() {
        if (currentLevel != null) {
            startLevel(currentLevel.id);
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (timer != null) timer.cancel();
    }

    public LiveData<Integer> getScore() { return score; }
    public LiveData<char[][]> getGrid() { return grid; }
    public LiveData<boolean[][]> getHighlighted() { return highlighted; }
    public LiveData<Long> getTimeRemaining() { return timeRemaining; }
    public LiveData<Boolean> getIsGameOver() { return isGameOver; }
    public LiveData<Boolean> getIsLevelComplete() { return isLevelComplete; }
    public LiveData<String> getStatusMessage() { return statusMessage; }
    public LiveData<String> getLastPredictedWord() { return lastPredictedWord; }
    public int getFinalScore() { return gameEngine.getScore(); }
    public int getCurrentLevelId() { return currentLevel != null ? currentLevel.id : 1; }
}
