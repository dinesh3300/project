package com.example.brainhemorrhage;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.bumptech.glide.Glide;

import com.google.android.material.textfield.TextInputEditText;

import java.util.regex.Pattern;
import com.example.brainhemorrhage.api.BaseResponse;
import com.example.brainhemorrhage.api.BrainScanApi;
import com.example.brainhemorrhage.api.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.io.File;
import java.io.InputStream;
import java.io.FileOutputStream;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public class SettingsFragment extends Fragment {

    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{6,}$"
    );

    private static final String PREFS_NAME = "NuerocheckPrefs";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_LANGUAGE = "language";
    private static final String KEY_FONT_SIZE = "font_size";
    private static final String KEY_DAILY_SUMMARY = "daily_summary";
    private static final String KEY_SOUND = "sound";
    private static final String KEY_VIBRATION = "vibration";

    private de.hdodenhof.circleimageview.CircleImageView profileImage;
    private com.google.android.material.floatingactionbutton.FloatingActionButton changePhotoButton;
    private ImageView themeIcon;
    private TextView languageText, doctorNameText, doctorDetailsText;

    private LinearLayout darkModeLayout, languageLayout, dailySummaryLayout, notifSoundLayout;
    private LinearLayout changePasswordLayout;
    private SwitchCompat darkModeSwitch, dailySummarySwitch, notifSoundSwitch;
    private Button editProfileButton;
    private View logoutButton;
    private TextView deleteAccountButton;
    private SharedPreferences prefs;

    private final ActivityResultLauncher<Intent> photoPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        profileImage.setImageURI(uri);
                        prefs.edit().putString("profile_image", uri.toString()).apply();
                        showToast("Uploading photo to server...", true);
                        uploadProfileToServer(doctorNameText.getText().toString(), doctorDetailsText.getText().toString(), uri);
                    }
                }
            });


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        initViews(view);
        loadSettings();
        setupListeners();

        androidx.appcompat.widget.Toolbar toolbar = view.findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(v).navigateUp());
        }

        return view;
    }

    private void initViews(View view) {
        profileImage           = view.findViewById(R.id.profileImage);
        changePhotoButton      = view.findViewById(R.id.changePhotoButton);
        doctorNameText         = view.findViewById(R.id.doctorNameText);
        doctorDetailsText      = view.findViewById(R.id.doctorDetailsText);
        editProfileButton      = view.findViewById(R.id.editProfileButton);

        
        themeIcon              = view.findViewById(R.id.themeIcon);

        darkModeLayout         = view.findViewById(R.id.darkModeLayout);
        darkModeSwitch         = view.findViewById(R.id.darkModeSwitch);
        languageLayout         = view.findViewById(R.id.languageLayout);
        languageText           = view.findViewById(R.id.languageText);
        dailySummaryLayout     = view.findViewById(R.id.dailySummaryLayout);
        dailySummarySwitch     = view.findViewById(R.id.dailySummarySwitch);
        notifSoundLayout       = view.findViewById(R.id.notifSoundLayout);
        notifSoundSwitch       = view.findViewById(R.id.notifSoundSwitch);

        changePasswordLayout   = view.findViewById(R.id.changePasswordLayout);
        logoutButton           = view.findViewById(R.id.logoutButton);
        deleteAccountButton    = view.findViewById(R.id.deleteAccountButton);
    }


    private void loadSettings() {
        boolean isDark      = prefs.getBoolean(KEY_DARK_MODE, false);
        String  language    = prefs.getString(KEY_LANGUAGE, "English");
        boolean dailySum    = prefs.getBoolean(KEY_DAILY_SUMMARY, true);
        boolean sound       = prefs.getBoolean(KEY_SOUND, true);

        darkModeSwitch.setChecked(isDark);
        dailySummarySwitch.setChecked(dailySum);
        notifSoundSwitch.setChecked(sound);
        updateThemeUI(isDark);
        languageText.setText(language);

        // Load profile data
        doctorNameText.setText(prefs.getString("name", "Doctor"));
        doctorDetailsText.setText(prefs.getString("specialty", "Senior Radiologist"));


        String photoUri = prefs.getString("profile_image", null);
        if (photoUri != null && !photoUri.isEmpty()) {
            String imageUrl = photoUri;
            if (!imageUrl.startsWith("http") && !imageUrl.startsWith("file") && !imageUrl.startsWith("content")) {
                imageUrl = RetrofitClient.BASE_URL + imageUrl;
            }
            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.logo)
                .error(R.drawable.logo)
                .into(profileImage);
        } else {
            profileImage.setImageResource(R.drawable.logo);
        }
    }


    private void setupListeners() {
        changePhotoButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            photoPickerLauncher.launch(intent);
        });

        editProfileButton.setOnClickListener(v -> showEditProfileDialog());

        darkModeLayout.setOnClickListener(v -> darkModeSwitch.toggle());
        darkModeSwitch.setOnCheckedChangeListener((btn, checked) -> {
            prefs.edit().putBoolean(KEY_DARK_MODE, checked).apply();
            updateThemeUI(checked);
            syncSetting("theme", checked ? "dark" : "light");
            AppCompatDelegate.setDefaultNightMode(
                    checked ? AppCompatDelegate.MODE_NIGHT_YES
                            : AppCompatDelegate.MODE_NIGHT_NO);
        });

        languageLayout.setOnClickListener(v -> showLanguageDialog());

        dailySummaryLayout.setOnClickListener(v -> dailySummarySwitch.toggle());
        dailySummarySwitch.setOnCheckedChangeListener((btn, checked) -> {
            prefs.edit().putBoolean(KEY_DAILY_SUMMARY, checked).apply();
            syncSetting("daily_summary", checked);
        });

        notifSoundLayout.setOnClickListener(v -> notifSoundSwitch.toggle());
        notifSoundSwitch.setOnCheckedChangeListener((btn, checked) -> {
            prefs.edit().putBoolean(KEY_SOUND, checked).apply();
            syncSetting("notif_sound", checked);
        });

        changePasswordLayout.setOnClickListener(v -> showChangePasswordDialog());
        
        logoutButton.setOnClickListener(v -> showLogoutDialog());

        deleteAccountButton.setOnClickListener(v -> showDeleteAccountDialog());
    }

    private void showEditProfileDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_profile, null);
        final TextInputEditText nameInput = dialogView.findViewById(R.id.nameInput);
        final TextInputEditText specialtyInput = dialogView.findViewById(R.id.specialtyInput);
        final TextInputEditText hospitalInput = dialogView.findViewById(R.id.hospitalInput);
        final TextInputEditText licenseInput = dialogView.findViewById(R.id.licenseInput);
        final TextInputEditText experienceInput = dialogView.findViewById(R.id.experienceInput);

        nameInput.setText(prefs.getString("name", ""));
        specialtyInput.setText(prefs.getString("specialty", ""));
        hospitalInput.setText(prefs.getString("hospital", ""));
        licenseInput.setText(prefs.getString("license_no", ""));
        experienceInput.setText(String.valueOf(prefs.getInt("experience", 0)));

        new AlertDialog.Builder(requireContext())
                .setTitle("Edit Profile")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = nameInput.getText() != null ? nameInput.getText().toString().trim() : "";
                    String spec = specialtyInput.getText() != null ? specialtyInput.getText().toString().trim() : "";
                    String hosp = hospitalInput.getText() != null ? hospitalInput.getText().toString().trim() : "";
                    String lic = licenseInput.getText() != null ? licenseInput.getText().toString().trim() : "";
                    int exp = 0;
                    try {
                        exp = Integer.parseInt(experienceInput.getText().toString());
                    } catch (Exception ignored) {}

                    if (name.isEmpty()) {
                        showToast("Name cannot be empty", false);
                        return;
                    }

                    doctorNameText.setText(name);
                    doctorDetailsText.setText(spec);

                    prefs.edit()
                            .putString("name", name)
                            .putString("specialty", spec)
                            .putString("hospital", hosp)
                            .putString("license_no", lic)
                            .putInt("experience", exp)
                            .apply();

                    uploadFullProfileToServer(name, spec, hosp, lic, exp);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void uploadFullProfileToServer(String name, String spec, String hosp, String lic, int exp) {
        String email = prefs.getString("email", "");
        if (email.isEmpty()) return;

        BrainScanApi api = RetrofitClient.getRetrofitInstance().create(BrainScanApi.class);
        
        // Update the API interface call to include all fields
        // Since I haven't updated BrainScanApi with the full fields yet, I'll do that now.
        // Actually, I'll use updateProfileTextOnly for simplicity if no image.
        
        // Let's first update BrainScanApi.java
    }

    private File compressImage(Uri uri) {
        try {
            InputStream input = requireContext().getContentResolver().openInputStream(uri);
            android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(input);
            if (input != null) input.close();
            
            if (bitmap == null) return null;
            
            int maxWidth = 1024;
            int maxHeight = 1024;
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            
            if (width > maxWidth || height > maxHeight) {
                float ratio = (float) width / (float) height;
                if (ratio > 1) {
                    width = maxWidth;
                    height = (int) (maxWidth / ratio);
                } else {
                    height = maxHeight;
                    width = (int) (maxHeight * ratio);
                }
                bitmap = android.graphics.Bitmap.createScaledBitmap(bitmap, width, height, true);
            }
            
            File compressedFile = new File(requireContext().getCacheDir(), "prof_compressed_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream out = new FileOutputStream(compressedFile);
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, out);
            out.flush();
            out.close();
            return compressedFile;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void uploadProfileToServer(String name, String specialty, Uri photoUri) {
        String email = prefs.getString("email", "");
        if (email.isEmpty()) return;

        final android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(requireContext());
        progressDialog.setMessage(photoUri != null ? "Uploading profile photo..." : "Saving profile...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        BrainScanApi api = RetrofitClient.getRetrofitInstance().create(BrainScanApi.class);

        String mobile = prefs.getString("mobile", "");
        String gender = prefs.getString("gender", "");
        String hosp = prefs.getString("hospital", "");
        String lic = prefs.getString("license_no", "");
        int exp = prefs.getInt("experience", 0);
        String dob = prefs.getString("dob", "");
        String addr = prefs.getString("address", "");

        if (photoUri == null) {
            api.updateProfileTextOnly(email, name != null ? name : "", mobile, gender, specialty != null ? specialty : "", hosp, lic, exp, dob, addr)
               .enqueue(new Callback<BaseResponse>() {
                   @Override
                   public void onResponse(Call<BaseResponse> call, Response<BaseResponse> response) {
                       progressDialog.dismiss();
                       if (response.isSuccessful() && response.body() != null && "success".equals(response.body().getStatus())) {
                           showToast("Profile updated successfully", true);
                       }
                   }
                   @Override
                   public void onFailure(Call<BaseResponse> call, Throwable t) {
                       progressDialog.dismiss();
                       showToast("Network error", false);
                   }
               });
            return;
        }

        RequestBody emailPart   = RequestBody.create(MultipartBody.FORM, email);
        RequestBody namePart    = RequestBody.create(MultipartBody.FORM, name != null ? name : "");
        RequestBody mobilePart  = RequestBody.create(MultipartBody.FORM, mobile);
        RequestBody genderPart  = RequestBody.create(MultipartBody.FORM, gender);
        RequestBody specPart    = RequestBody.create(MultipartBody.FORM, specialty != null ? specialty : "");
        RequestBody hospPart    = RequestBody.create(MultipartBody.FORM, hosp);
        RequestBody licPart     = RequestBody.create(MultipartBody.FORM, lic);
        RequestBody expPart     = RequestBody.create(MultipartBody.FORM, String.valueOf(exp));
        RequestBody dobPart     = RequestBody.create(MultipartBody.FORM, dob);
        RequestBody addrPart    = RequestBody.create(MultipartBody.FORM, addr);

        MultipartBody.Part imagePart = null;
        try {
            File file = compressImage(photoUri);
            if (file != null && file.exists()) {
                RequestBody requestFile = RequestBody.create(MediaType.parse("image/jpeg"), file);
                imagePart = MultipartBody.Part.createFormData("profile_image", file.getName(), requestFile);
            }
        } catch (Exception e) { e.printStackTrace(); }

        if (imagePart == null) {
            progressDialog.dismiss();
            showToast("Failed to process image", false);
            return;
        }

        api.updateProfile(emailPart, namePart, mobilePart, genderPart, specPart, hospPart, licPart, expPart, dobPart, addrPart, imagePart)
           .enqueue(new Callback<BaseResponse>() {
                @Override
                public void onResponse(Call<BaseResponse> call, Response<BaseResponse> response) {
                    progressDialog.dismiss();
                    if (response.isSuccessful() && response.body() != null && "success".equals(response.body().getStatus())) {
                        if (response.body().getProfile_image() != null) {
                            prefs.edit().putString("profile_image", response.body().getProfile_image()).apply();
                        }
                        showToast("Profile photo updated", true);
                    }
                }
                @Override
                public void onFailure(Call<BaseResponse> call, Throwable t) {
                    progressDialog.dismiss();
                    showToast("Upload failed", false);
                }
           });
    }



    private void syncSetting(String key, Object value) {
        String email = prefs.getString("email", "");
        if (email.isEmpty()) return;

        BrainScanApi api = RetrofitClient.getRetrofitInstance().create(BrainScanApi.class);
        String theme = key.equals("theme") ? (String) value : null;
        String lang = key.equals("language") ? (String) value : null;
        String ds = key.equals("daily_summary") ? String.valueOf(value) : null;
        String ns = key.equals("notif_sound") ? String.valueOf(value) : null;
        String nv = key.equals("notif_vibration") ? String.valueOf(value) : null;
        
        api.updateSettings(email, theme, lang, ds, ns, nv).enqueue(new Callback<BaseResponse>() {
            @Override
            public void onResponse(@NonNull Call<BaseResponse> call, @NonNull Response<BaseResponse> response) {
            }
            @Override
            public void onFailure(@NonNull Call<BaseResponse> call, @NonNull Throwable t) {
            }
        });
    }

    private void updateThemeUI(boolean isDark) {
        themeIcon.setImageResource(isDark ? R.drawable.ic_moon : R.drawable.ic_sun);
    }


    private void showLanguageDialog() {
        // India-only: English + 7 major Indian languages
        String[] languages = {
                "English",
                "Hindi (हिन्दी)",
                "Bengali (বাংলা)",
                "Tamil (தமிழ்)",
                "Telugu (తెలుగు)",
                "Marathi (मराठी)",
                "Kannada (ಕನ್ನಡ)",
                "Gujarati (ગુજરાતી)"
        };
        String current = prefs.getString(KEY_LANGUAGE, "English");
        int sel = 0;
        for (int i = 0; i < languages.length; i++) {
            if (languages[i].equals(current)) { sel = i; break; }
        }
        new AlertDialog.Builder(requireContext())
                .setTitle("Select Language")
                .setSingleChoiceItems(languages, sel, (dialog, which) -> {
                    prefs.edit().putString(KEY_LANGUAGE, languages[which]).apply();
                    languageText.setText(languages[which]);
                    syncSetting("language", languages[which]);
                    showToast("Language: " + languages[which], true);
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showChangePasswordDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        TextInputEditText currentPwdInput  = dialogView.findViewById(R.id.currentPasswordInput);
        TextInputEditText newPwdInput      = dialogView.findViewById(R.id.newPasswordInput);
        TextInputEditText confirmPwdInput  = dialogView.findViewById(R.id.confirmPasswordInput);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Change Password")
                .setView(dialogView)
                .setPositiveButton("Change", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String current = currentPwdInput.getText() != null
                    ? currentPwdInput.getText().toString().trim() : "";
            String newPwd  = newPwdInput.getText() != null
                    ? newPwdInput.getText().toString() : "";
            String confirm = confirmPwdInput.getText() != null
                    ? confirmPwdInput.getText().toString() : "";

            if (current.isEmpty() || newPwd.isEmpty() || confirm.isEmpty()) {
                ErrorDialogHelper.show(getContext(), "Missing Fields", "Please fill in all fields.");
                return;
            }
            if (!PASSWORD_PATTERN.matcher(newPwd).matches()) {
                ErrorDialogHelper.show(getContext(), "Password Requirements",
                        "Password must be at least 6 characters and include an uppercase letter, a number, and a symbol.");
                return;
            }
            if (!newPwd.equals(confirm)) {
                ErrorDialogHelper.show(getContext(), "Mismatch", "Passwords do not match.");
                return;
            }

            dialog.dismiss();
            requestPasswordChangeOtp(newPwd);
        });
    }

    private void requestPasswordChangeOtp(final String newPwd) {
        final String email = prefs.getString("email", "");
        if (email.isEmpty()) return;

        showToast("Sending verification code...", true);

        BrainScanApi api = RetrofitClient.getRetrofitInstance().create(BrainScanApi.class);
        api.sendOtp(email, "update_pwd").enqueue(new Callback<BaseResponse>() {
            @Override
            public void onResponse(Call<BaseResponse> call, Response<BaseResponse> response) {
                if (response.isSuccessful() && response.body() != null && "success".equals(response.body().getStatus())) {
                    showToast(response.body().getMessage(), true);
                    showPasswordChangeOtpDialog(email, newPwd);
                } else {
                    String msg = (response.body() != null && response.body().getMessage() != null)
                            ? response.body().getMessage() : "Failed to send OTP verification code";
                    showToast(msg, false);
                }
            }

            @Override
            public void onFailure(Call<BaseResponse> call, Throwable t) {
                showToast("Network error: " + t.getMessage(), false);
            }
        });
    }

    private void showPasswordChangeOtpDialog(final String email, final String newPwd) {
        new OtpInputDialog(requireContext(), email, "update_pwd",
                verifiedCode -> updatePasswordOnBackend(email, verifiedCode, newPwd))
                .show();
    }


    private void updatePasswordOnBackend(final String email, final String code, final String newPwd) {
        showToast("Updating password...", true);

        BrainScanApi api = RetrofitClient.getRetrofitInstance().create(BrainScanApi.class);
        api.resetPassword(email, code, newPwd).enqueue(new Callback<BaseResponse>() {
            @Override
            public void onResponse(Call<BaseResponse> call, Response<BaseResponse> response) {
                if (response.isSuccessful() && response.body() != null && "success".equals(response.body().getStatus())) {
                    showToast("Password updated successfully", true);
                } else {
                    String msg = (response.body() != null && response.body().getMessage() != null)
                            ? response.body().getMessage() : "Failed to update password";
                    showToast(msg, false);
                }
            }

            @Override
            public void onFailure(Call<BaseResponse> call, Throwable t) {
                showToast("Network error: " + t.getMessage(), false);
            }
        });
    }

    private void showInfoDialog(String title, String message) {
        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    prefs.edit().clear().apply();
                    showToast("Logged out successfully", true);
                    View v = getView();
                    if (v != null) {
                        try {
                            Navigation.findNavController(v).navigate(R.id.action_settings_to_login);
                        } catch (Exception e) {
                            requireActivity().finish();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDeleteAccountDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to permanently delete your account? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // Get logged in email
                    String userEmail = prefs.getString("email", "");

                    BrainScanApi api = RetrofitClient.getRetrofitInstance().create(BrainScanApi.class);
                    api.deleteAccount(userEmail).enqueue(new Callback<BaseResponse>() {
                        @Override
                        public void onResponse(Call<BaseResponse> call, Response<BaseResponse> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                if ("success".equals(response.body().getStatus())) {
                                    prefs.edit().clear().apply();
                                    showToast("Account deleted successfully", true);
                                    View v = getView();
                                    if (v != null) {
                                        try {
                                            Navigation.findNavController(v).navigate(R.id.action_settings_to_login);
                                        } catch (Exception e) {
                                            requireActivity().finish();
                                        }
                                    }
                                } else {
                                    showToast(response.body().getMessage(), false);
                                }
                            } else {
                                showToast("Server error", false);
                            }
                        }

                        @Override
                        public void onFailure(Call<BaseResponse> call, Throwable t) {
                            showToast("Network error: " + t.getMessage(), false);
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showToast(String msg, boolean success) {
        CustomToast.show(getView(), msg, success);
    }


}
