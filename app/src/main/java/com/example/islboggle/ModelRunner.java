package com.example.islboggle;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;

/**
 * Loads model.tflite and performs inference on a sequence of frames.
 */
public class ModelRunner {

    private static final String TAG = "ModelRunner";
    private static final String MODEL_FILE = "model.tflite";

    private final Interpreter interpreter;
    private final int numClasses;
    private final int[] inputShape;
    private final int[] outputShape;

    public ModelRunner(Context context) throws IOException {
        MappedByteBuffer modelBuffer = loadModelFile(context);
        Interpreter.Options opts = new Interpreter.Options();
        opts.setNumThreads(4);
        this.interpreter = new Interpreter(modelBuffer, opts);

        this.inputShape = interpreter.getInputTensor(0).shape();
        this.outputShape = interpreter.getOutputTensor(0).shape();
        this.numClasses = outputShape[outputShape.length - 1];
        
        Log.i(TAG, "Model Loaded Successfully");
        Log.i(TAG, "Input Shape: " + Arrays.toString(inputShape));
        Log.i(TAG, "Output Shape: " + Arrays.toString(outputShape));
        Log.i(TAG, "Detected Classes: " + numClasses);
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
     * @param sequence Input sequence
     * @return probability array
     */
    public float[] run(float[][][][] sequence) {
        if (sequence == null) return null;

        try {
            // We use the shape detected from the model itself to allocate output
            if (outputShape.length == 2) {
                float[][] output = new float[outputShape[0]][outputShape[1]];
                interpreter.run(sequence, output);
                return output[0];
            } else if (outputShape.length == 3) {
                // Handle models with [Batch, Sequence, Classes] shape
                float[][][] output = new float[outputShape[0]][outputShape[1]][outputShape[2]];
                interpreter.run(sequence, output);
                // Return the last prediction in the sequence by default
                return output[0][outputShape[1] - 1];
            } else {
                Log.e(TAG, "Unsupported output shape: " + Arrays.toString(outputShape));
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Inference failed", e);
            return null;
        }
    }

    public void close() {
        if (interpreter != null) interpreter.close();
    }
}
