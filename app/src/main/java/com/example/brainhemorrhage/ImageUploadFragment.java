package com.example.brainhemorrhage;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.google.android.material.card.MaterialCardView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ImageUploadFragment extends Fragment {

    private MaterialCardView galleryCard;
    private MaterialCardView cameraCard;
    private MaterialCardView previewCard;
    private ImageView selectedImageView;
    private TextView imageStatusText;
    private Button continueButton;

    private String patientId;
    private String patientName;
    private String patientAge;
    private String patientGender;
    private boolean isExistingPatient;

    /** URI of the photo taken by the camera (stored in cache) */
    private Uri cameraImageUri;

    /** The final URI chosen by user (from gallery or camera) */
    private Uri selectedImageUri;

    // --- Activity Result Launchers ---

    /** Picks an image from the gallery */
    private final ActivityResultLauncher<Intent> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        onImageSelected(uri);
                    }
                }
            });

    /** Captures a photo using the camera */
    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && cameraImageUri != null) {
                    onImageSelected(cameraImageUri);
                }
            });

    /** Requests CAMERA permission */
    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    launchCamera();
                } else {
                    CustomToast.show(getView(),
                            "Camera permission is required to take photos.", false);
                }
            });

    /** Requests READ_MEDIA_IMAGES (Android 13+) or READ_EXTERNAL_STORAGE (older) */
    private final ActivityResultLauncher<String> galleryPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    launchGallery();
                } else {
                    CustomToast.show(getView(),
                            "Storage permission is required to access the gallery.", false);
                }
            });

    // ---

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_image_upload, container, false);

        // Toolbar
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        // Views
        galleryCard = view.findViewById(R.id.galleryCard);
        cameraCard = view.findViewById(R.id.cameraCard);
        previewCard = view.findViewById(R.id.previewCard);
        selectedImageView = view.findViewById(R.id.selectedImageView);
        imageStatusText = view.findViewById(R.id.imageStatusText);
        continueButton = view.findViewById(R.id.continueButton);

        // Patient data from arguments
        Bundle args = getArguments();
        if (args != null) {
            patientId = args.getString("patientId");
            patientName = args.getString("patientName");
            patientAge = args.getString("patientAge");
            patientGender = args.getString("patientGender");
            isExistingPatient = args.getBoolean("isExistingPatient", false);
        }

        // Gallery button
        galleryCard.setOnClickListener(v -> checkGalleryPermissionAndLaunch());

        // Camera button
        cameraCard.setOnClickListener(v -> checkCameraPermissionAndLaunch());

        // Continue to processing
        continueButton.setOnClickListener(v -> {
            if (selectedImageUri == null) {
                CustomToast.show(getView(),
                        "Please select or capture an image first.", false);
                return;
            }
            Bundle bundle = new Bundle();
            bundle.putString("patientId", patientId);
            bundle.putString("patientName", patientName);
            bundle.putString("patientAge", patientAge);
            bundle.putString("patientGender", patientGender);
            bundle.putBoolean("isExistingPatient", isExistingPatient);
            bundle.putString("imageUri", selectedImageUri.toString());
            Navigation.findNavController(v).navigate(R.id.action_upload_to_processing, bundle);
        });

        return view;
    }

    // --- Permission & launch helpers ---

    private void checkGalleryPermissionAndLaunch() {
        String permission;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ uses READ_MEDIA_IMAGES
            permission = Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        if (ContextCompat.checkSelfPermission(requireContext(), permission)
                == PackageManager.PERMISSION_GRANTED) {
            launchGallery();
        } else {
            galleryPermissionLauncher.launch(permission);
        }
    }

    private void checkCameraPermissionAndLaunch() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        galleryLauncher.launch(intent);
    }

    private void launchCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Only launch if there's a camera app available
        if (intent.resolveActivity(requireActivity().getPackageManager()) == null) {
            CustomToast.show(getView(), "No camera app found on this device.", false);
            return;
        }

        // Create a temp file for the camera output via FileProvider
        try {
            File photoFile = createTempImageFile();
            cameraImageUri = FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    photoFile);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
            cameraLauncher.launch(intent);
        } catch (IOException e) {
            CustomToast.show(getView(), "Could not create image file.", false);
        }
    }

    /** Creates a uniquely-named temporary file in the app's external picture directory. */
    private File createTempImageFile() throws IOException {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String fileName = "SCAN_" + timestamp + "_";
        File storageDir = requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(fileName, ".jpg", storageDir);
    }

    // --- Image selected callback ---

    private void onImageSelected(Uri uri) {
        selectedImageUri = uri;

        // Show preview
        previewCard.setVisibility(View.VISIBLE);
        selectedImageView.setImageURI(uri);
        imageStatusText.setText("Image ready for processing");
        imageStatusText.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.success_green));

        // Highlight gallery / camera card
        galleryCard.setCardBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.medical_blue_10));
        cameraCard.setCardBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.medical_blue_10));

        // Enable the continue button
        continueButton.setEnabled(true);
    }
}
