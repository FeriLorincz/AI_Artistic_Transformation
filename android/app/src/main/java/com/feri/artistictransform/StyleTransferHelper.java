package com.feri.artistictransform;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.SystemClock;
import android.util.Log;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.TensorOperator;
import org.tensorflow.lite.support.common.TensorProcessor;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StyleTransferHelper {

    private static final String TAG = "StyleTransferHelper";
    private static final String MODEL_PATH = "model.tflite";
    private static final int IMAGE_SIZE = 256;

    private final Context context;
    private Interpreter interpreter;
    private TensorImage inputImageBuffer;
    private TensorBuffer outputImageBuffer;
    private final TensorProcessor probabilityProcessor;

    public StyleTransferHelper(Context context) {
        this.context = context;

        probabilityProcessor = new TensorProcessor.Builder()
                .add(new NormalizeOp(0, 1))
                .build();

        setupInterpreter();
    }

    private void setupInterpreter() {
        try {
            String modelPath = "model.tflite";
            Log.d(TAG, "Attempting to load model from: " + modelPath);

            // Verifică dacă fișierul există și printează lista de assets
            String[] assets = context.getAssets().list("");
            Log.d(TAG, "Available assets:");
            for (String asset : assets) {
                Log.d(TAG, "- " + asset);
            }

            // Încarcă modelul
            MappedByteBuffer modelFile = FileUtil.loadMappedFile(context, modelPath);
            Log.d(TAG, "Model file size: " + modelFile.capacity() + " bytes");

            // Configurare optimizer pentru GPU dacă este disponibil
            Interpreter.Options options = new Interpreter.Options();
            options.setNumThreads(4);  // Utilizăm 4 thread-uri pentru procesare

            interpreter = new Interpreter(modelFile, options);
            Log.d(TAG, "Interpreter created successfully");

            // Verificăm dimensiunile tensorului de intrare
            int[] inputShape = interpreter.getInputTensor(0).shape();
            Log.d(TAG, "Input tensor shape: " +
                    inputShape[0] + "x" + inputShape[1] + "x" +
                    inputShape[2] + "x" + inputShape[3]);

            // Verificăm dimensiunile tensorului de ieșire
            int[] outputShape = interpreter.getOutputTensor(0).shape();
            Log.d(TAG, "Output tensor shape: " +
                    outputShape[0] + "x" + outputShape[1] + "x" +
                    outputShape[2] + "x" + outputShape[3]);

            // Inițializăm buffer-ele
            inputImageBuffer = new TensorImage(DataType.FLOAT32);
            outputImageBuffer = TensorBuffer.createFixedSize(outputShape, DataType.FLOAT32);

            Log.d(TAG, "Setup completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error in setupInterpreter: " + e.getMessage(), e);
            e.printStackTrace();
        }
    }

    public List<Bitmap> generateStyles(Bitmap image) {
        List<Bitmap> results = new ArrayList<>();

        try {
            Log.d(TAG, "Starting style generation...");
            Log.d(TAG, "Input image dimensions: " + image.getWidth() + "x" + image.getHeight());

            // Procesăm imaginea originală
            Bitmap originalStyled = processImage(image);
            if (originalStyled != null) {
                Log.d(TAG, "Original style processed successfully");
                results.add(originalStyled);

                // Adăugăm variația rotită
                Matrix matrix = new Matrix();
                matrix.postRotate(90);
                Bitmap rotatedImage = Bitmap.createBitmap(image, 0, 0,
                        image.getWidth(), image.getHeight(), matrix, true);
                Bitmap rotatedStyled = processImage(rotatedImage);
                if (rotatedStyled != null) {
                    results.add(rotatedStyled);
                    Log.d(TAG, "Added rotated variation");
                }

                // Adăugăm variația în oglindă
                matrix.reset();
                matrix.preScale(-1.0f, 1.0f);
                Bitmap flippedImage = Bitmap.createBitmap(image, 0, 0,
                        image.getWidth(), image.getHeight(), matrix, true);
                Bitmap flippedStyled = processImage(flippedImage);
                if (flippedStyled != null) {
                    results.add(flippedStyled);
                    Log.d(TAG, "Added flipped variation");
                }

                // Adăugăm variația cu contrast modificat
                ColorMatrix cm = new ColorMatrix();
                cm.setSaturation(1.5f);
                ColorMatrixColorFilter filter = new ColorMatrixColorFilter(cm);
                Bitmap contrastedImage = Bitmap.createBitmap(
                        image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(contrastedImage);
                Paint paint = new Paint();
                paint.setColorFilter(filter);
                canvas.drawBitmap(image, 0, 0, paint);
                Bitmap contrastedStyled = processImage(contrastedImage);
                if (contrastedStyled != null) {
                    results.add(contrastedStyled);
                    Log.d(TAG, "Added contrasted variation");
                }

                Log.d(TAG, "Generated " + results.size() + " variations");
            } else {
                Log.e(TAG, "Failed to process original image");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in generateStyles: " + e.getMessage());
            e.printStackTrace();
        }

        return results;
    }

    private Bitmap processImage(Bitmap image) {
        if (interpreter == null) {
            Log.e(TAG, "Interpreter not initialized");
            return null;
        }

        try {
            Log.d(TAG, "Starting image processing");

            // Redimensionăm imaginea
            Bitmap resizedImage = Bitmap.createScaledBitmap(image, IMAGE_SIZE, IMAGE_SIZE, true);

            // Creăm array de intrare
            float[][][][] inputArray = new float[1][IMAGE_SIZE][IMAGE_SIZE][3];
            int[] pixels = new int[IMAGE_SIZE * IMAGE_SIZE];
            resizedImage.getPixels(pixels, 0, IMAGE_SIZE, 0, 0, IMAGE_SIZE, IMAGE_SIZE);

            // Convertim pixelii în format float
            for (int i = 0; i < pixels.length; i++) {
                int pixel = pixels[i];
                int y = i / IMAGE_SIZE;
                int x = i % IMAGE_SIZE;

                inputArray[0][y][x][0] = ((pixel >> 16) & 0xFF) / 255.0f;
                inputArray[0][y][x][1] = ((pixel >> 8) & 0xFF) / 255.0f;
                inputArray[0][y][x][2] = (pixel & 0xFF) / 255.0f;
            }

            // Creăm array de ieșire
            float[][][][] outputArray = new float[1][IMAGE_SIZE][IMAGE_SIZE][3];

            try {
                // Rulăm modelul
                Object[] inputs = new Object[]{inputArray};
                Map<Integer, Object> outputs = new HashMap<>();
                outputs.put(0, outputArray);

                interpreter.runForMultipleInputsOutputs(inputs, outputs);

                // Convertim rezultatul înapoi în Bitmap
                Bitmap outputBitmap = Bitmap.createBitmap(IMAGE_SIZE, IMAGE_SIZE, Bitmap.Config.ARGB_8888);
                for (int i = 0; i < pixels.length; i++) {
                    int y = i / IMAGE_SIZE;
                    int x = i % IMAGE_SIZE;

                    int red = (int) (outputArray[0][y][x][0] * 255);
                    int green = (int) (outputArray[0][y][x][1] * 255);
                    int blue = (int) (outputArray[0][y][x][2] * 255);

                    pixels[i] = (0xFF << 24) | (red << 16) | (green << 8) | blue;
                }
                outputBitmap.setPixels(pixels, 0, IMAGE_SIZE, 0, 0, IMAGE_SIZE, IMAGE_SIZE);

                return outputBitmap;
            } catch (Exception e) {
                Log.e(TAG, "Error running model: " + e.getMessage());
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing image: " + e.getMessage());
            return null;
        }
    }

    private Bitmap createRotatedVariation(Bitmap original, float degrees) {
        try {
            android.graphics.Matrix matrix = new android.graphics.Matrix();
            matrix.postRotate(degrees);
            Bitmap rotated = Bitmap.createBitmap(original, 0, 0,
                    original.getWidth(), original.getHeight(), matrix, true);
            return processImage(rotated);
        } catch (Exception e) {
            Log.e(TAG, "Error creating rotation variation", e);
            return null;
        }
    }

    private Bitmap createFlippedVariation(Bitmap original) {
        try {
            android.graphics.Matrix matrix = new android.graphics.Matrix();
            matrix.preScale(-1.0f, 1.0f);
            Bitmap flipped = Bitmap.createBitmap(original, 0, 0,
                    original.getWidth(), original.getHeight(), matrix, true);
            return processImage(flipped);
        } catch (Exception e) {
            Log.e(TAG, "Error creating flipped variation", e);
            return null;
        }
    }

    private Bitmap createContrastedVariation(Bitmap original) {
        try {
            android.graphics.ColorMatrix cm = new android.graphics.ColorMatrix();
            cm.setSaturation(1.5f);
            android.graphics.ColorMatrixColorFilter filter = new android.graphics.ColorMatrixColorFilter(cm);

            Bitmap contrasted = original.copy(Bitmap.Config.ARGB_8888, true);
            android.graphics.Canvas canvas = new android.graphics.Canvas(contrasted);
            android.graphics.Paint paint = new android.graphics.Paint();
            paint.setColorFilter(filter);
            canvas.drawBitmap(contrasted, 0, 0, paint);

            return processImage(contrasted);
        } catch (Exception e) {
            Log.e(TAG, "Error creating contrast variation", e);
            return null;
        }
    }

    public void close() {
        if (interpreter != null) {
            interpreter.close();
            interpreter = null;
        }
    }
}