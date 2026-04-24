package com.example.islboggle.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LevelRepository {
    private static final String PREF_NAME = "islboggle_prefs";
    private static final String KEY_PREFIX = "level_unlocked_";

    private final SharedPreferences prefs;
    private final List<Level> levels = new ArrayList<>();

    public LevelRepository(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        initLevels();
    }

    private void initLevels() {
        // 5 minutes in milliseconds
        long fiveMinutesMs = 5 * 60 * 1000L;

        // LEVEL 1
        char[][] grid1 = {
                {'B', 'P', 'X', 'Y'},
                {'A', 'L', 'L', 'T'},
                {'N', 'G', 'Z', 'X'},
                {'E', 'Y', 'K', 'X'}
        };
        List<WordPath> words1 = Arrays.asList(
                new WordPath("BAG", Arrays.asList(new int[]{0,0}, new int[]{1,0}, new int[]{2,1})),
                new WordPath("BALL", Arrays.asList(new int[]{0,0}, new int[]{1,0}, new int[]{1,1}, new int[]{1,2})),
                new WordPath("PLANE", Arrays.asList(new int[]{0,1}, new int[]{1,1}, new int[]{1,0}, new int[]{2,0}, new int[]{3,0}))
        );
        levels.add(new Level(1, grid1, words1, fiveMinutesMs, isLevelUnlocked(1, true)));

        // LEVEL 2
        char[][] grid2 = {
                {'G', 'U', 'N', 'J'},
                {'R', 'H', 'X', 'I'},
                {'E', 'E', 'N', 'Q'},
                {'U', 'L', 'L', 'X'},
                {'J', 'X', 'O', 'X'}
        };
        List<WordPath> words2 = Arrays.asList(
                new WordPath("GUN", Arrays.asList(new int[]{0,0}, new int[]{0,1}, new int[]{0,2})),
                new WordPath("HELLO", Arrays.asList(new int[]{1,1}, new int[]{2,1}, new int[]{3,1}, new int[]{3,2}, new int[]{4,2})),
                new WordPath("GREEN", Arrays.asList(new int[]{0,0}, new int[]{1,0}, new int[]{2,0}, new int[]{2,1}, new int[]{2,2}))
        );
        levels.add(new Level(2, grid2, words2, fiveMinutesMs, isLevelUnlocked(2, false)));

        // LEVEL 3
        char[][] grid3 = {
                {'G', 'O', 'O', 'D'},
                {'S', 'C', 'S', 'O'},
                {'W', 'I', 'S', 'R'},
                {'A', 'T', 'E', 'S'}
        };
        List<WordPath> words3 = Arrays.asList(
                new WordPath("GOOD", Arrays.asList(new int[]{0,0}, new int[]{0,1}, new int[]{0,2}, new int[]{0,3})),
                new WordPath("SCISSORS", Arrays.asList(new int[]{1,0}, new int[]{1,1}, new int[]{2,1}, new int[]{1,2}, new int[]{2,2}, new int[]{1,3}, new int[]{2,3}, new int[]{3,3})),
                new WordPath("WATER", Arrays.asList(new int[]{2,0}, new int[]{3,0}, new int[]{3,1}, new int[]{3,2}, new int[]{2,3}))
        );
        levels.add(new Level(3, grid3, words3, fiveMinutesMs, isLevelUnlocked(3, false)));
    }

    private boolean isLevelUnlocked(int id, boolean defaultState) {
        return prefs.getBoolean(KEY_PREFIX + id, defaultState);
    }

    public List<Level> getLevels() {
        return levels;
    }

    public Level getLevel(int id) {
        for (Level l : levels) {
            if (l.id == id) return l;
        }
        return null;
    }

    public void unlockNextLevel(int currentLevelId) {
        Level next = getLevel(currentLevelId + 1);
        if (next != null) {
            next.isUnlocked = true;
            prefs.edit().putBoolean(KEY_PREFIX + next.id, true).apply();
        }
    }
}
