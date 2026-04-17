package com.example.islboggle.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

public class LevelRepository {
    private static final String PREF_NAME = "islboggle_prefs";
    private static final String KEY_PREFIX = "level_unlocked_";

    private final SharedPreferences prefs;
    private final List<Level> levels = new ArrayList<>();

    public LevelRepository(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        
        // Initialize dynamic levels
        levels.add(new Level(1, 1, 300_000L, 4, isLevelUnlocked(1, true))); // Always unlocked
        levels.add(new Level(2, 2, 240_000L, 4, isLevelUnlocked(2, false)));
        levels.add(new Level(3, 3, 180_000L, 4, isLevelUnlocked(3, false)));
        levels.add(new Level(4, 4, 150_000L, 4, isLevelUnlocked(4, false)));
        levels.add(new Level(5, 5, 120_000L, 4, isLevelUnlocked(5, false)));
        levels.add(new Level(6, 6, 90_000L, 4, isLevelUnlocked(6, false)));
        levels.add(new Level(7, 7, 75_000L, 4, isLevelUnlocked(7, false)));
        levels.add(new Level(8, 8, 60_000L, 4, isLevelUnlocked(8, false)));
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
