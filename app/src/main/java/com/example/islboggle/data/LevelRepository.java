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
        // LEVEL 1
        char[][] grid1 = {
                {'G', 'I', 'R', 'L'},
                {'D', 'O', 'O', 'F'},
                {'F', 'E', 'X', 'M'},
                {'Z', 'U', 'E', 'R'}
        };
        List<WordPath> words1 = Arrays.asList(
                new WordPath("GIRL", Arrays.asList(new int[]{0,0}, new int[]{0,1}, new int[]{0,2}, new int[]{0,3})),
                new WordPath("FOOD", Arrays.asList(new int[]{1,3}, new int[]{1,2}, new int[]{1,1}, new int[]{1,0})),
                new WordPath("DEER", Arrays.asList(new int[]{1,0}, new int[]{2,1}, new int[]{3,2}, new int[]{3,3}))
        );
        levels.add(new Level(1, grid1, words1, 60000L, isLevelUnlocked(1, true)));

        // LEVEL 2
        char[][] grid2 = {
                {'B', 'I', 'R', 'A'},
                {'O', 'O', 'K', 'I'},
                {'F', 'E', 'L', 'N'},
                {'Z', 'V', 'O', 'R'}
        };
        List<WordPath> words2 = Arrays.asList(
                new WordPath("BOOK", Arrays.asList(new int[]{0,0}, new int[]{1,0}, new int[]{1,1}, new int[]{1,2})),
                new WordPath("RAIN", Arrays.asList(new int[]{0,2}, new int[]{0,3}, new int[]{1,3}, new int[]{2,3})),
                new WordPath("LOVE", Arrays.asList(new int[]{2,2}, new int[]{3,2}, new int[]{3,1}, new int[]{2,1}))
        );
        levels.add(new Level(2, grid2, words2, 60000L, isLevelUnlocked(2, false)));

        // LEVEL 3 (Same as 2 per request)
        levels.add(new Level(3, grid2, words2, 60000L, isLevelUnlocked(3, false)));

        // LEVEL 4
        char[][] grid4 = {
                {'B', 'A', 'L', 'A'},
                {'E', 'C', 'L', 'I'},
                {'A', 'E', 'L', 'N'},
                {'R', 'V', 'O', 'R'}
        };
        List<WordPath> words4 = Arrays.asList(
                new WordPath("BALL", Arrays.asList(new int[]{0,0}, new int[]{0,1}, new int[]{0,2}, new int[]{1,2})),
                new WordPath("CALL", Arrays.asList(new int[]{1,1}, new int[]{0,1}, new int[]{0,2}, new int[]{1,2})),
                new WordPath("BEAR", Arrays.asList(new int[]{0,0}, new int[]{1,0}, new int[]{2,0}, new int[]{3,0}))
        );
        levels.add(new Level(4, grid4, words4, 60000L, isLevelUnlocked(4, false)));
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
