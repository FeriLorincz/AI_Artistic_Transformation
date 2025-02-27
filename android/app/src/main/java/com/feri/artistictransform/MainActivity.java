package com.feri.artistictransform;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_CAMERA_REQUEST = 1;
    private static final int PERMISSION_GALLERY_REQUEST = 2;
    private static final int REQUEST_IMAGE_CAPTURE = 3;
    private static final int REQUEST_PICK_IMAGE = 4;

    private ImageView previewImageView;
    private ViewPager2 stylesPager;
    private StylePagerAdapter stylePagerAdapter;
    private FloatingActionButton cameraButton;
    private StyleTransferHelper styleTransferHelper;
    private RecyclerView resultsRecyclerView;
    private StyleResultsAdapter styleResultsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupViewPager();
        setupClickListeners();
    }


    private void initializeViews() {
        previewImageView = findViewById(R.id.previewImageView);
        stylesPager = findViewById(R.id.stylesPager);
        cameraButton = findViewById(R.id.cameraButton);
    }

    private void setupViewPager() {
        Log.d(TAG, "Setting up ViewPager");
        stylePagerAdapter = new StylePagerAdapter();
        stylesPager.setAdapter(stylePagerAdapter);
        stylesPager.setOffscreenPageLimit(1);

        // Adăugăm callback pentru a vedea când se schimbă pagina
        stylesPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                Log.d(TAG, "Page selected: " + position);
            }
        });
    }

    private void setupRecyclerView() {
        styleResultsAdapter = new StyleResultsAdapter();
        resultsRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        resultsRecyclerView.setAdapter(styleResultsAdapter);
    }

    private void setupClickListeners() {
        cameraButton.setOnClickListener(v -> checkCameraPermission());
        previewImageView.setOnClickListener(v -> checkGalleryPermission());
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    PERMISSION_CAMERA_REQUEST);
        } else {
            openCamera();
        }
    }

    private void checkGalleryPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                        PERMISSION_GALLERY_REQUEST);
            } else {
                openGallery();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        PERMISSION_GALLERY_REQUEST);
            } else {
                openGallery();
            }
        }
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_PICK_IMAGE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == PERMISSION_CAMERA_REQUEST) {
                openCamera();
            } else if (requestCode == PERMISSION_GALLERY_REQUEST) {
                openGallery();
            }
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            try {
                Bitmap imageBitmap = null;
                int rotation = 0;

                if (requestCode == REQUEST_IMAGE_CAPTURE && data != null) {
                    imageBitmap = (Bitmap) data.getExtras().get("data");
                    // Camera photos are usually correctly oriented
                } else if (requestCode == REQUEST_PICK_IMAGE && data != null) {
                    Uri imageUri = data.getData();
                    // Obținem rotația pentru imagini din galerie
                    rotation = ImageProcessorHelper.getImageRotation(this, imageUri);
                    imageBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                }

                if (imageBitmap != null) {
                    // Aplicăm rotația și afișăm imaginea
                    Bitmap correctedImage = ImageProcessorHelper.preprocessImage(imageBitmap, rotation);
                    previewImageView.setImageBitmap(correctedImage);

                    // Procesăm imaginea corectată
                    processImage(correctedImage);
                }
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error loading image: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void processImage(Bitmap originalImage) {
        Log.d(TAG, "Starting image processing");

        if (styleTransferHelper == null) {
            try {
                Log.d(TAG, "Creating new StyleTransferHelper");
                styleTransferHelper = new StyleTransferHelper(this);
            } catch (Exception e) {
                Log.e(TAG, "Error creating StyleTransferHelper: " + e.getMessage(), e);
                showError("Error initializing style transfer: " + e.getMessage());
                return;
            }
        }

        // Procesăm imaginea în background
        new Thread(() -> {
            try {
                Log.d(TAG, "Starting background processing");
                final List<Bitmap> styledResults = styleTransferHelper.generateStyles(originalImage);
                Log.d(TAG, "Processing completed, results size: " +
                        (styledResults != null ? styledResults.size() : "null"));

                // Actualizăm UI pe thread-ul principal
                runOnUiThread(() -> {
                    try {
                        if (styledResults != null && !styledResults.isEmpty()) {
                            Log.d(TAG, "Updating UI with results");
                            stylePagerAdapter.setStyles(styledResults);
                            stylesPager.setCurrentItem(0, false);
                        } else {
                            Log.e(TAG, "No results generated");
                            showError("No style results generated");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error updating UI: " + e.getMessage(), e);
                        showError("Error updating UI: " + e.getMessage());
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error in background processing: " + e.getMessage(), e);
                showError("Error processing image: " + e.getMessage());
            }
        }).start();
    }

    private void showError(String message) {
        runOnUiThread(() ->
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        );
    }
}