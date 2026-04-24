package com.example.islboggle.data;

import java.util.List;

public class Level {
    public final int id;
    public final char[][] grid;
    public final List<WordPath> words;
    public final long timeLimitMs;
    public boolean isUnlocked;

    public Level(int id, char[][] grid, List<WordPath> words, long timeLimitMs, boolean isUnlocked) {
        this.id = id;
        this.grid = grid;
        this.words = words;
        this.timeLimitMs = timeLimitMs;
        this.isUnlocked = isUnlocked;
    }
}
