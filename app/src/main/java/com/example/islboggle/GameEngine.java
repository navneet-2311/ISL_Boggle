package com.example.islboggle;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/**
 * Boggle-style game logic: builds a word from stable letters,
 * validates against dictionary, and tracks score.
 */
public class GameEngine {

    private static final String TAG = "GameEngine";
    private static final String DICT_FILE = "dictionary.txt";

    private final Set<String> dictionary = new HashSet<>();
    private final StringBuilder currentWord = new StringBuilder();
    private int score = 0;

    public GameEngine(Context context) {
        loadDictionary(context);
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

    public synchronized void appendLetter(String letter) {
        if (letter == null || letter.isEmpty()) return;
        currentWord.append(letter.toUpperCase());
    }

    /** Returns >0 (points awarded) if current word is valid, else 0. Resets word on success. */
    public synchronized int tryCommitWord() {
        String w = currentWord.toString().toUpperCase();
        if (w.length() >= 2 && dictionary.contains(w)) {
            int points = w.length();
            score += points;
            currentWord.setLength(0);
            Log.i(TAG, "Valid word: " + w + " (+" + points + ")");
            return points;
        }
        return 0;
    }

    public synchronized void resetWord() {
        currentWord.setLength(0);
    }

    public synchronized String getCurrentWord() { return currentWord.toString(); }
    public synchronized int getScore()          { return score; }
    public int getDictionarySize()              { return dictionary.size(); }
}
