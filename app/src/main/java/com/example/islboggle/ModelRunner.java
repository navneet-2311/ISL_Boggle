package com.example.islboggle;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Loads model.tflite and performs inference on a sequence of frames.
 * Input shape:  [1, 150, 42, 3]
 * Output shape: [1, N] where N is number of words
 */
public class ModelRunner {

    private static final String TAG = "ModelRunner";
    private static final String MODEL_FILE = "model.tflite";

    private final Interpreter interpreter;
    private final int numClasses;

    public ModelRunner(Context context) throws IOException {
        MappedByteBuffer modelBuffer = loadModelFile(context);
        Interpreter.Options opts = new Interpreter.Options();
        opts.setNumThreads(4);
        this.interpreter = new Interpreter(modelBuffer, opts);

        int[] outShape = interpreter.getOutputTensor(0).shape();
        this.numClasses = outShape[outShape.length - 1];
        Log.i(TAG, "Model loaded. Output classes: " + numClasses);
    }

    private MappedByteBuffer loadModelFile(Context context) throws IOException {
        try (AssetFileDescriptor fd = context.getAssets().openFd(MODEL_FILE);
             FileInputStream fis = new FileInputStream(fd.getFileDescriptor())) {
            FileChannel channel = fis.getChannel();
            return channel.map(FileChannel.MapMode.READ_ONLY, fd.getStartOffset(), fd.getDeclaredLength());
        }
    }

    public int getNumClasses() {
        return numClasses;
    }

    /**
     * @param sequence float[1][150][42][3]
     * @return probability array of length numClasses
     */
    public float[] run(float[][][][] sequence) {
        if (sequence == null) return null;

        try {
            float[][] output = new float[1][numClasses];
            interpreter.run(sequence, output);
            return output[0];
        } catch (Exception e) {
            Log.e(TAG, "Inference failed", e);
            return null;
        }
    }

    public void close() {
        if (interpreter != null) interpreter.close();
    }
}
