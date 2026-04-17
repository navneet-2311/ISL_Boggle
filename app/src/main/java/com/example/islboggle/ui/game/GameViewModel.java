package com.example.islboggle.ui.game;

import android.app.Application;
import android.os.CountDownTimer;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.islboggle.GameEngine;

public class GameViewModel extends AndroidViewModel {
    private final GameEngine gameEngine;
    
    private final MutableLiveData<String> currentWord = new MutableLiveData<>("");
    private final MutableLiveData<Integer> score = new MutableLiveData<>(0);
    private final MutableLiveData<char[][]> grid = new MutableLiveData<>();
    private final MutableLiveData<Long> timeRemaining = new MutableLiveData<>(0L);
    private final MutableLiveData<Boolean> isGameOver = new MutableLiveData<>(false);
    private final MutableLiveData<String> statusMessage = new MutableLiveData<>("");

    private CountDownTimer timer;

    public GameViewModel(@NonNull Application application) {
        super(application);
        gameEngine = new GameEngine(application);
        grid.setValue(gameEngine.getGrid());
    }

    public void startLevel(long timeLimitMs) {
        if (timer != null) timer.cancel();
        isGameOver.setValue(false);
        gameEngine.generateGrid();
        grid.setValue(gameEngine.getGrid());
        
        timer = new CountDownTimer(timeLimitMs, 1000) {
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

    public void appendLetter(String letter) {
        if (Boolean.TRUE.equals(isGameOver.getValue())) return;
        gameEngine.appendLetter(letter);
        currentWord.setValue(gameEngine.getCurrentWord());
    }

    public void deleteLetter() {
        if (Boolean.TRUE.equals(isGameOver.getValue())) return;
        gameEngine.deleteLastLetter();
        currentWord.setValue(gameEngine.getCurrentWord());
    }

    public void clearWord() {
        if (Boolean.TRUE.equals(isGameOver.getValue())) return;
        gameEngine.resetWord();
        currentWord.setValue(gameEngine.getCurrentWord());
    }

    public void submitWord() {
        if (Boolean.TRUE.equals(isGameOver.getValue())) return;
        int pts = gameEngine.tryCommitWord();
        if (pts > 0) {
            score.setValue(gameEngine.getScore());
            statusMessage.setValue("+" + pts + " pts!");
        } else {
            statusMessage.setValue("Invalid Word");
        }
        currentWord.setValue(gameEngine.getCurrentWord());
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (timer != null) timer.cancel();
    }

    public LiveData<String> getCurrentWord() { return currentWord; }
    public LiveData<Integer> getScore() { return score; }
    public LiveData<char[][]> getGrid() { return grid; }
    public LiveData<Long> getTimeRemaining() { return timeRemaining; }
    public LiveData<Boolean> getIsGameOver() { return isGameOver; }
    public LiveData<String> getStatusMessage() { return statusMessage; }
    public int getFinalScore() { return gameEngine.getScore(); }
}
