package com.example.islboggle;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Maintains a sliding window of frames for the sequence-based model.
 * Each frame is float[42][3].
 * Buffer size is 150 frames.
 */
public class FrameBuffer {
    public static final int BUFFER_SIZE = 150;
    public static final int NUM_LANDMARKS = 42;
    public static final int COORDS = 3;

    private final Deque<float[][]> buffer = new ArrayDeque<>(BUFFER_SIZE);

    public synchronized void addFrame(float[][] frame) {
        if (frame == null) {
            frame = new float[NUM_LANDMARKS][COORDS]; // Zero fill
        }
        buffer.addLast(frame);
        if (buffer.size() > BUFFER_SIZE) {
            buffer.removeFirst();
        }
    }

    public synchronized boolean isFull() {
        return buffer.size() == BUFFER_SIZE;
    }

    public synchronized float[][][][] getFlattenedInput() {
        float[][][][] input = new float[1][BUFFER_SIZE][NUM_LANDMARKS][COORDS];
        int i = 0;
        for (float[][] frame : buffer) {
            input[0][i] = frame;
            i++;
        }
        // If not full, the rest remains zeros (padding)
        return input;
    }

    public synchronized void clear() {
        buffer.clear();
    }
}
