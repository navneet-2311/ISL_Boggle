package com.example.islboggle;

import android.util.Log;

import com.example.islboggle.data.Level;
import com.example.islboggle.data.WordPath;

import java.util.HashSet;
import java.util.Set;

public class GameEngine {

    private static final String TAG = "GameEngine";

    private Level currentLevel;
    private final Set<String> foundWords = new HashSet<>();
    private final boolean[][] highlighted;
    private int score = 0;

    public GameEngine() {
        highlighted = new boolean[4][4];
    }

    public void startLevel(Level level) {
        this.currentLevel = level;
        this.foundWords.clear();
        this.score = 0;
        resetHighlights();
    }

    private void resetHighlights() {
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                highlighted[r][c] = false;
            }
        }
    }

    public char[][] getGrid() {
        return currentLevel != null ? currentLevel.grid : new char[4][4];
    }

    public boolean[][] getHighlighted() {
        return highlighted;
    }

    /**
     * @param word The word predicted by the ML model
     * @return points gained (0 if invalid/already found)
     */
    public synchronized int tryMatchWord(String word) {
        if (currentLevel == null || word == null || word.isEmpty()) return 0;

        String w = word.toUpperCase();
        if (foundWords.contains(w)) return 0;

        for (WordPath wp : currentLevel.words) {
            if (wp.word.equals(w)) {
                foundWords.add(w);
                highlightWordOnGrid(wp);
                int points = 10;
                score += points;
                Log.i(TAG, "MATCH! Word: " + w + " | Score: " + score);
                return points;
            }
        }
        return 0;
    }

    private void highlightWordOnGrid(WordPath wp) {
        if (wp.path == null) return;
        for (int[] pos : wp.path) {
            if (pos.length == 2) {
                int r = pos[0];
                int c = pos[1];
                if (r >= 0 && r < 4 && c >= 0 && c < 4) {
                    highlighted[r][c] = true;
                }
            }
        }
    }

    public boolean isLevelComplete() {
        return currentLevel != null && foundWords.size() == currentLevel.words.size();
    }

    public int getScore() {
        return score;
    }

    public int getFoundCount() {
        return foundWords.size();
    }
}
