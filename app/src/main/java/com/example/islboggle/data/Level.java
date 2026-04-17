package com.example.islboggle.data;

public class Level {
    public final int id;
    public final int difficulty;
    public final long timeLimitMs;
    public final int gridSize;
    public boolean isUnlocked;

    public Level(int id, int difficulty, long timeLimitMs, int gridSize, boolean isUnlocked) {
        this.id = id;
        this.difficulty = difficulty;
        this.timeLimitMs = timeLimitMs;
        this.gridSize = gridSize;
        this.isUnlocked = isUnlocked;
    }
}
