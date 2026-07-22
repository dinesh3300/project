package com.example.brainhemorrhage;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import android.content.Context;
import android.content.SharedPreferences;
import com.google.android.material.textfield.TextInputEditText;
import com.example.brainhemorrhage.api.BaseResponse;
import com.example.brainhemorrhage.api.BrainScanApi;
import com.example.brainhemorrhage.api.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.regex.Pattern;

public class SignupFragment extends Fragment {

    // Password: 6+ chars, at least one uppercase, one digit, one symbol
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{6,}$"
    );

    private TextInputEditText fullNameInput, emailInput, mobileInput, genderInput, passwordInput;
    private Button createAccountButton;
    private TextView loginText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_signup, container, false);

        fullNameInput   = view.findViewById(R.id.nameInput);
        emailInput      = view.findViewById(R.id.emailInput);
        mobileInput     = view.findViewById(R.id.mobileInput);
        genderInput     = view.findViewById(R.id.genderInput);
        passwordInput   = view.findViewById(R.id.passwordInput);
        createAccountButton = view.findViewById(R.id.signupButton);
        loginText       = view.findViewById(R.id.loginText);


        genderInput.setOnClickListener(v -> showGenderPicker());
        createAccountButton.setOnClickListener(v -> handleSignup());
        loginText.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        // Apply touch bounce scaling animations
        AnimationHelper.applyBouncePress(createAccountButton);
        AnimationHelper.applyBouncePress(loginText);

        // Stagger sequence slide-in entry animation
        AnimationHelper.animateViewsInSequence(
            view.findViewById(R.id.logoCard),
            view.findViewById(R.id.signupCardContainer)
        );

        // Gentle floating animation on form container
        float density = getResources().getDisplayMetrics().density;
        AnimationHelper.applyFloatingEffect(view.findViewById(R.id.signupCardContainer), 6f * density, 4000, 200);

        return view;
    }

    private void showGenderPicker() {
        String[] genders = {"Male", "Female", "Other"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Select Gender")
                .setItems(genders, (dialog, which) -> genderInput.setText(genders[which]))
                .show();
    }

    private void handleSignup() {
        String name     = fullNameInput.getText().toString().trim();
        String email    = emailInput.getText().toString().trim();
        String mobile   = mobileInput.getText().toString().trim();
        String gender   = genderInput.getText().toString().trim();
        String password = passwordInput.getText().toString();

        if (name.isEmpty() || email.isEmpty() || mobile.isEmpty() || gender.isEmpty() || password.isEmpty()) {
            ErrorDialogHelper.show(getContext(), "Missing Fields", "Please fill in all fields to register.");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            ErrorDialogHelper.show(getContext(), "Invalid Email", "Please enter a valid email address.");
            return;
        }

        if (mobile.length() < 7 || mobile.length() > 15) {
            ErrorDialogHelper.show(getContext(), "Invalid Phone Number", "Please enter a valid mobile number.");
            return;
        }

        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            ErrorDialogHelper.show(getContext(), "Password Requirements",
                    "Password must be at least 6 characters and include an uppercase letter, a number, and a symbol.");
            return;
        }

        // Request OTP code from server first
        createAccountButton.setEnabled(false);
        createAccountButton.setText("Sending OTP...");

        com.example.brainhemorrhage.api.BrainScanApi api = com.example.brainhemorrhage.api.RetrofitClient.getRetrofitInstance().create(com.example.brainhemorrhage.api.BrainScanApi.class);
        api.sendOtp(email, "signup").enqueue(new retrofit2.Callback<com.example.brainhemorrhage.api.BaseResponse>() {
            @Override
            public void onResponse(retrofit2.Call<com.example.brainhemorrhage.api.BaseResponse> call, retrofit2.Response<com.example.brainhemorrhage.api.BaseResponse> response) {
                createAccountButton.setEnabled(true);
                createAccountButton.setText("Sign Up");

                if (response.isSuccessful() && response.body() != null && "success".equals(response.body().getStatus())) {
                    CustomToast.show(getView(), response.body().getMessage(), true);
                    showOtpDialog(name, email, mobile, gender, password);
                } else {
                    String msg = (response.body() != null && response.body().getMessage() != null) 
                            ? response.body().getMessage() : "Failed to send OTP verification code";
                    ErrorDialogHelper.show(getContext(), "Registration Failed", msg);
                }
            }

            @Override
            public void onFailure(retrofit2.Call<com.example.brainhemorrhage.api.BaseResponse> call, Throwable t) {
                createAccountButton.setEnabled(true);
                createAccountButton.setText("Sign Up");
                ErrorDialogHelper.show(getContext(), "Connection Failed", "Failed to connect to server: " + t.getLocalizedMessage());
            }
        });
    }

    private void showOtpDialog(final String name, final String email, final String mobile,
                               final String gender, final String password) {
        new OtpInputDialog(requireContext(), email, "signup",
                // verifiedCode is the exact 6-digit code the user typed — pass it
                // to registerUser so signup.php can verify it atomically.
                verifiedCode -> registerUser(name, email, mobile, gender, password, verifiedCode))
                .show();
    }


    private void registerUser(final String name, final String email, final String mobile,
                              final String gender, final String password, final String otpCode) {
        createAccountButton.setEnabled(false);
        createAccountButton.setText("Creating account...");

        com.example.brainhemorrhage.api.BrainScanApi api = com.example.brainhemorrhage.api.RetrofitClient.getRetrofitInstance().create(com.example.brainhemorrhage.api.BrainScanApi.class);
        api.signup(name, email, mobile, gender, password, otpCode).enqueue(new retrofit2.Callback<com.example.brainhemorrhage.api.BaseResponse>() {
            @Override
            public void onResponse(retrofit2.Call<com.example.brainhemorrhage.api.BaseResponse> call, retrofit2.Response<com.example.brainhemorrhage.api.BaseResponse> response) {
                createAccountButton.setEnabled(true);
                createAccountButton.setText("Sign Up");

                if (response.isSuccessful() && response.body() != null) {
                    com.example.brainhemorrhage.api.BaseResponse baseResponse = response.body();
                    if ("success".equals(baseResponse.getStatus())) {
                        SharedPreferences prefs = requireActivity().getSharedPreferences("NuerocheckPrefs", Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString("email", email);
                        editor.putString("name", name);
                        editor.putString("mobile", mobile);
                        editor.putString("gender", gender);
                        editor.apply();

                        View fragmentView = getView();
                        if (fragmentView != null) {
                            Navigation.findNavController(fragmentView).navigate(R.id.action_signup_to_welcome);
                        }
                    } else {
                        // Log the FULL server response for debugging (contains debug_otp_rows etc.)
                        try {
                            String rawJson = response.raw().peekBody(Long.MAX_VALUE).string();
                            Log.e("SIGNUP_DEBUG", "Server response: " + rawJson);
                        } catch (Exception ex) {
                            Log.e("SIGNUP_DEBUG", "Could not read raw body: " + ex.getMessage());
                        }
                        ErrorDialogHelper.show(getContext(), "Registration Failed",
                                baseResponse.getMessage() != null ? baseResponse.getMessage() : "Signup failed");
                    }
                } else {
                    String errorMsg = "Server returned an error (HTTP " + response.code() + ")";
                    try {
                        if (response.errorBody() != null) {
                            errorMsg += "\n\nDetails:\n" + response.errorBody().string();
                        }
                    } catch (IOException ignored) {}
                    ErrorDialogHelper.show(getContext(), "Server Error", errorMsg);
                }
            }

            @Override
            public void onFailure(retrofit2.Call<com.example.brainhemorrhage.api.BaseResponse> call, Throwable t) {
                createAccountButton.setEnabled(true);
                createAccountButton.setText("Sign Up");
                ErrorDialogHelper.show(getContext(), "Network Error", "Network error: " + t.getLocalizedMessage());
            }
        });
    }
}
