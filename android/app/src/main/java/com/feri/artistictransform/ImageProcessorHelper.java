package com.feri.artistictransform;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;

public class ImageProcessorHelper {

    private static final int MODEL_IMAGE_SIZE = 256;

    public static Bitmap preprocessImage(Bitmap image, int rotation) {
        // Mai întâi corectăm rotația
        if (rotation != 0) {
            Matrix matrix = new Matrix();
            matrix.postRotate(rotation);
            image = Bitmap.createBitmap(image, 0, 0,
                    image.getWidth(), image.getHeight(), matrix, true);
        }

        // Apoi redimensionăm
        float aspectRatio = image.getWidth() / (float) image.getHeight();
        int targetWidth = MODEL_IMAGE_SIZE;
        int targetHeight = MODEL_IMAGE_SIZE;

        if (aspectRatio > 1) {
            targetHeight = (int) (MODEL_IMAGE_SIZE / aspectRatio);
        } else {
            targetWidth = (int) (MODEL_IMAGE_SIZE * aspectRatio);
        }

        return Bitmap.createScaledBitmap(image, targetWidth, targetHeight, true);
    }

    public static int getImageRotation(Context context, Uri imageUri) {
        try {
            if (imageUri.getScheme().equals("content")) {
                String[] projection = {MediaStore.Images.ImageColumns.ORIENTATION};
                Cursor cursor = context.getContentResolver().query(
                        imageUri, projection, null, null, null);

                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        return cursor.getInt(0);
                    }
                    cursor.close();
                }
            }
        } catch (Exception e) {
            Log.e("ImageProcessor", "Error getting image rotation: " + e.getMessage());
        }
        return 0;
    }

    public static TensorImage bitmapToTensorImage(Bitmap bitmap) {
        ImageProcessor tfImageProcessor = new ImageProcessor.Builder()
                .add(new ResizeOp(MODEL_IMAGE_SIZE, MODEL_IMAGE_SIZE, ResizeOp.ResizeMethod.BILINEAR))
                .build();

        TensorImage tensorImage = TensorImage.fromBitmap(bitmap);
        return tfImageProcessor.process(tensorImage);
    }

    public static Bitmap postprocessImage(TensorImage tensorImage) {
        return tensorImage.getBitmap();
    }
}
