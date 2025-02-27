package com.feri.artistictransform;

import android.content.Context;
import android.util.Log;
import java.nio.MappedByteBuffer;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;

public class ModelInspector {

    private static final String TAG = "ModelInspector";

    public static void inspectModel(Context context) {
        try {
            MappedByteBuffer modelBuffer = FileUtil.loadMappedFile(context, "model.tflite");
            Interpreter.Options options = new Interpreter.Options();

            try (Interpreter interpreter = new Interpreter(modelBuffer, options)) {
                // Verifică dimensiunile tensorilor
                Log.d(TAG, "Model loaded successfully");
                Log.d(TAG, "Number of input tensors: " + interpreter.getInputTensorCount());
                Log.d(TAG, "Number of output tensors: " + interpreter.getOutputTensorCount());

                // Pentru fiecare tensor de intrare
                for (int i = 0; i < interpreter.getInputTensorCount(); i++) {
                    int[] shape = interpreter.getInputTensor(i).shape();
                    String shapeStr = "";
                    for (int dim : shape) {
                        shapeStr += dim + "x";
                    }
                    shapeStr = shapeStr.substring(0, shapeStr.length() - 1);
                    Log.d(TAG, "Input tensor " + i + " shape: " + shapeStr);
                    Log.d(TAG, "Input tensor " + i + " dataType: " + interpreter.getInputTensor(i).dataType());
                }

                // Pentru fiecare tensor de ieșire
                for (int i = 0; i < interpreter.getOutputTensorCount(); i++) {
                    int[] shape = interpreter.getOutputTensor(i).shape();
                    String shapeStr = "";
                    for (int dim : shape) {
                        shapeStr += dim + "x";
                    }
                    shapeStr = shapeStr.substring(0, shapeStr.length() - 1);
                    Log.d(TAG, "Output tensor " + i + " shape: " + shapeStr);
                    Log.d(TAG, "Output tensor " + i + " dataType: " + interpreter.getOutputTensor(i).dataType());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error inspecting model: " + e.getMessage(), e);
        }
    }
}
