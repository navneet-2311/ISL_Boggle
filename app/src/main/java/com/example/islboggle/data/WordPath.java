package com.example.islboggle.data;

import java.util.List;

public class WordPath {
    public final String word;
    public final List<int[]> path; // List of [row, col]

    public WordPath(String word, List<int[]> path) {
        this.word = word;
        this.path = path;
    }
}
