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
 * Loads model.tflite from assets and performs inference on a float[42].
 * Input shape:  [1, 42]
 * Output shape: [1, N]
 */
public class ModelRunner {

    private static final String TAG = "ModelRunner";
    private static final String MODEL_FILE = "model.tflite";

    private final Interpreter interpreter;
    private final int numClasses;

    public ModelRunner(Context context) throws IOException {
        MappedByteBuffer modelBuffer = loadModelFile(context);
        Interpreter.Options opts = new Interpreter.Options();
        opts.setNumThreads(2);
        this.interpreter = new Interpreter(modelBuffer, opts);

        int[] outShape = interpreter.getOutputTensor(0).shape(); // [1, N]
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
     * @param flat42 float array of length 42
     * @return probability array of length numClasses
     */
    public float[] run(float[] flat42) {
        if (flat42 == null || flat42.length != LandmarkProcessor.FLAT_SIZE) {
            Log.w(TAG, "Invalid input length: " + (flat42 == null ? -1 : flat42.length));
            return null;
        }
        float[][] input  = new float[1][LandmarkProcessor.FLAT_SIZE];
        System.arraycopy(flat42, 0, input[0], 0, LandmarkProcessor.FLAT_SIZE);

        float[][] output = new float[1][numClasses];
        try {
            interpreter.run(input, output);
        } catch (Exception e) {
            Log.e(TAG, "Inference failed", e);
            return null;
        }
        return output[0];
    }

    public void close() {
        if (interpreter != null) interpreter.close();
    }
}
