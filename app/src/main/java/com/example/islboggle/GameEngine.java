package com.example.islboggle;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class GameEngine {

    private static final String TAG = "GameEngine";
    private static final String DICT_FILE = "dictionary.txt";

    private final Set<String> dictionary = new HashSet<>();
    private final StringBuilder currentWord = new StringBuilder();
    private int score = 0;
    
    private char[][] grid;
    private final int gridSize = 4;
    private final Random random = new Random();
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    public GameEngine(Context context) {
        loadDictionary(context);
        generateGrid();
    }

    private void loadDictionary(Context context) {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(context.getAssets().open(DICT_FILE), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                String w = line.trim().toUpperCase();
                if (!w.isEmpty()) dictionary.add(w);
            }
            Log.i(TAG, "Loaded dictionary: " + dictionary.size() + " words");
        } catch (IOException e) {
            Log.e(TAG, "Failed to load dictionary", e);
        }
    }

    public void generateGrid() {
        grid = new char[gridSize][gridSize];
        for (int r = 0; r < gridSize; r++) {
            for (int c = 0; c < gridSize; c++) {
                grid[r][c] = ALPHABET.charAt(random.nextInt(ALPHABET.length()));
            }
        }
    }

    public char[][] getGrid() {
        return grid;
    }

    public synchronized void appendLetter(String letter) {
        if (letter == null || letter.isEmpty()) return;
        currentWord.append(letter.toUpperCase());
    }

    public synchronized void deleteLastLetter() {
        if (currentWord.length() > 0) {
            currentWord.deleteCharAt(currentWord.length() - 1);
        }
    }

    public synchronized void resetWord() {
        currentWord.setLength(0);
    }

    public synchronized int tryCommitWord() {
        String w = currentWord.toString().toUpperCase();
        if (w.length() >= 2 && dictionary.contains(w) && isWordInGrid(w)) {
            int points = w.length() * 10;
            score += points;
            currentWord.setLength(0);
            Log.i(TAG, "Valid word: " + w + " (+" + points + ")");
            return points;
        }
        return 0;
    }

    private boolean isWordInGrid(String word) {
        if (word == null || word.isEmpty()) return false;
        for (int r = 0; r < gridSize; r++) {
            for (int c = 0; c < gridSize; c++) {
                if (grid[r][c] == word.charAt(0)) {
                    boolean[][] visited = new boolean[gridSize][gridSize];
                    if (dfs(r, c, word, 0, visited)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean dfs(int r, int c, String word, int index, boolean[][] visited) {
        if (index == word.length()) return true;
        if (r < 0 || c < 0 || r >= gridSize || c >= gridSize) return false;
        if (visited[r][c] || grid[r][c] != word.charAt(index)) return false;

        visited[r][c] = true;

        int[] dr = {-1, -1, -1, 0, 0, 1, 1, 1};
        int[] dc = {-1, 0, 1, -1, 1, -1, 0, 1};
        
        for (int i = 0; i < 8; i++) {
            if (dfs(r + dr[i], c + dc[i], word, index + 1, visited)) {
                return true;
            }
        }

        visited[r][c] = false;
        return false;
    }

    public synchronized String getCurrentWord() { return currentWord.toString(); }
    public synchronized int getScore()          { return score; }
    public int getDictionarySize()              { return dictionary.size(); }
}
