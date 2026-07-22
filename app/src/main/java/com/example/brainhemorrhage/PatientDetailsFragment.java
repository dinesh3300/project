package com.example.brainhemorrhage;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

public class PatientDetailsFragment extends Fragment {

    private EditText nameInput, ageInput, genderInput, idInput, notesInput;
    private MaterialCardView uploadCard;
    private ImageView selectedScanImage;
    private LinearLayout uploadPlaceholder;
    private Button analyzeButton;

    private Uri cameraImageUri;
    private Uri selectedImageUri;

    // --- Activity Result Launchers ---

    private final ActivityResultLauncher<Intent> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) onImageSelected(uri);
                }
            });

    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && cameraImageUri != null) {
                    onImageSelected(cameraImageUri);
                }
            });

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) launchCamera();
                else CustomToast.show(getView(), "Camera permission is required.", false);
            });

    private final ActivityResultLauncher<String> galleryPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) launchGallery();
                else CustomToast.show(getView(), "Storage permission is required.", false);
            });

    // ---

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_patient_details, container, false);

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        idInput = view.findViewById(R.id.patientIdInput);
        nameInput = view.findViewById(R.id.patientNameInput);
        ageInput = view.findViewById(R.id.patientAgeInput);
        genderInput = view.findViewById(R.id.patientGenderInput);
        notesInput = view.findViewById(R.id.patientNotesInput);
        uploadCard = view.findViewById(R.id.uploadCard);
        selectedScanImage = view.findViewById(R.id.selectedScanImage);
        uploadPlaceholder = view.findViewById(R.id.uploadPlaceholder);
        analyzeButton = view.findViewById(R.id.analyzeButton);

        // Pre-fill if existing patient
        if (getArguments() != null && getArguments().getBoolean("isExistingPatient", false)) {
            String id = getArguments().getString("patientId");
            String name = getArguments().getString("patientName");
            String age = getArguments().getString("patientAge");
            String gender = getArguments().getString("patientGender");
            
            if (id != null) {
                idInput.setText(id);
                idInput.setEnabled(false);
            }
            
            nameInput.setText(name);
            nameInput.setEnabled(false);
            
            if (age != null && !age.equals("--")) {
                ageInput.setText(age);
                ageInput.setEnabled(false);
            }
            
            if (gender != null && !gender.equals("--")) {
                genderInput.setText(gender);
                genderInput.setClickable(false);
                genderInput.setFocusable(false);
            }
        } else {
            // Auto-generate a default patient ID for new patients
            String autoId = "PAT_" + (System.currentTimeMillis() % 100000000);
            idInput.setText(autoId);
        }

        genderInput.setOnClickListener(v -> showGenderPicker());

        // Show source chooser when upload card is tapped
        uploadCard.setOnClickListener(v -> showImageSourceDialog());

        analyzeButton.setOnClickListener(v -> handleAnalysis());

        return view;
    }

    // --- Image source selection ---

    private void showImageSourceDialog() {
        String[] options = {"Choose from Gallery", "Take a Photo"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Select Image Source")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) checkGalleryPermissionAndLaunch();
                    else checkCameraPermissionAndLaunch();
                })
                .show();
    }

    private void checkGalleryPermissionAndLaunch() {
        String permission = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
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
        if (intent.resolveActivity(requireActivity().getPackageManager()) == null) {
            CustomToast.show(getView(), "No camera app found.", false);
            return;
        }
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

    private File createTempImageFile() throws IOException {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        File storageDir = requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile("SCAN_" + timestamp + "_", ".jpg", storageDir);
    }

    private void onImageSelected(Uri uri) {
        selectedImageUri = uri;
        selectedScanImage.setImageURI(uri);
        selectedScanImage.setVisibility(View.VISIBLE);
        uploadPlaceholder.setVisibility(View.GONE);
        uploadCard.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.success_green));
    }

    // ---

    private void showGenderPicker() {
        String[] genders = {"Male", "Female", "Other"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Select Gender")
                .setItems(genders, (dialog, which) -> genderInput.setText(genders[which]))
                .show();
    }

    private void handleAnalysis() {
        String id = idInput.getText().toString().trim();
        String name = nameInput.getText().toString().trim();
        String age = ageInput.getText().toString().trim();

        if (name.isEmpty() || age.isEmpty()) {
            CustomToast.show(getView(), "Please fill in patient details", false);
            return;
        }

        if (selectedImageUri == null) {
            CustomToast.show(getView(), "Please select or capture a scan image", false);
            return;
        }

        Bundle bundle = new Bundle();
        if (getArguments() != null) {
            bundle.putAll(getArguments());
        }
        if (!id.isEmpty()) {
            bundle.putString("patientId", id);
        }
        bundle.putString("patientName", name);
        bundle.putString("patientAge", age);
        bundle.putString("patientGender", genderInput.getText().toString().trim());
        bundle.putString("patientNotes", notesInput.getText().toString().trim());
        bundle.putString("imageUri", selectedImageUri.toString());

        Navigation.findNavController(requireView()).navigate(R.id.action_patientDetails_to_processing, bundle);
    }
}
