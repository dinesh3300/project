package com.example.brainhemorrhage;

import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.google.android.material.textfield.TextInputEditText;
import android.app.AlertDialog;
import com.example.brainhemorrhage.api.BaseResponse;
import com.example.brainhemorrhage.api.BrainScanApi;
import com.example.brainhemorrhage.api.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.regex.Pattern;

public class ForgotPasswordFragment extends Fragment {

    // Password: 6+ chars, at least one uppercase, one digit, one symbol
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{6,}$"
    );

    private LinearLayout formContainer, successContainer;
    private TextInputEditText emailInput, phoneInput, newPasswordInput, confirmPasswordInput;
    private Button resetPasswordButton, backToLoginButton;
    private TextView backToLoginText, successMessage;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_forgot_password, container, false);

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        formContainer        = view.findViewById(R.id.formContainer);
        successContainer     = view.findViewById(R.id.successContainer);
        emailInput           = view.findViewById(R.id.emailInput);
        phoneInput           = view.findViewById(R.id.phoneInput);
        newPasswordInput     = view.findViewById(R.id.newPasswordInput);
        confirmPasswordInput = view.findViewById(R.id.confirmPasswordInput);
        resetPasswordButton  = view.findViewById(R.id.resetPasswordButton);
        backToLoginButton    = view.findViewById(R.id.backToLoginButton);
        backToLoginText      = view.findViewById(R.id.backToLoginText);
        successMessage       = view.findViewById(R.id.successMessage);

        resetPasswordButton.setOnClickListener(v -> handleResetPassword());
        backToLoginText.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());
        backToLoginButton.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        return view;
    }

    private void handleResetPassword() {
        String email       = emailInput.getText().toString().trim();
        String phone       = phoneInput.getText().toString().trim();
        String newPassword = newPasswordInput.getText().toString();
        String confirmPass = confirmPasswordInput.getText().toString();

        // Empty-field guard
        if (email.isEmpty() || phone.isEmpty() || newPassword.isEmpty() || confirmPass.isEmpty()) {
            ErrorDialogHelper.show(getContext(), "Missing Fields", "Please fill in all fields.");
            return;
        }

        // Email format
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            ErrorDialogHelper.show(getContext(), "Invalid Email", "Please enter a valid email address.");
            return;
        }

        // Phone length
        if (phone.length() < 7 || phone.length() > 15) {
            ErrorDialogHelper.show(getContext(), "Invalid Phone Number", "Please enter a valid phone number.");
            return;
        }

        // Password complexity
        if (!PASSWORD_PATTERN.matcher(newPassword).matches()) {
            ErrorDialogHelper.show(getContext(), "Password Requirements",
                    "Password must be at least 6 characters and include an uppercase letter, a number, and a symbol.");
            return;
        }

        // Passwords match
        if (!newPassword.equals(confirmPass)) {
            ErrorDialogHelper.show(getContext(), "Mismatch", "Passwords do not match.");
            return;
        }

        // Send OTP via API
        resetPasswordButton.setEnabled(false);
        resetPasswordButton.setText("Sending OTP...");

        com.example.brainhemorrhage.api.BrainScanApi api = com.example.brainhemorrhage.api.RetrofitClient.getRetrofitInstance().create(com.example.brainhemorrhage.api.BrainScanApi.class);
        api.sendOtp(email, "forgot_pwd").enqueue(new Callback<BaseResponse>() {
            @Override
            public void onResponse(Call<BaseResponse> call, Response<BaseResponse> response) {
                resetPasswordButton.setEnabled(true);
                resetPasswordButton.setText("Reset Password");

                if (response.isSuccessful() && response.body() != null && "success".equals(response.body().getStatus())) {
                    CustomToast.show(getView(), response.body().getMessage(), true);
                    showOtpDialog(email, newPassword);
                } else {
                    String msg = (response.body() != null && response.body().getMessage() != null)
                            ? response.body().getMessage() : "Failed to send OTP verification code";
                    CustomToast.show(getView(), msg, false);
                }
            }

            @Override
            public void onFailure(Call<BaseResponse> call, Throwable t) {
                resetPasswordButton.setEnabled(true);
                resetPasswordButton.setText("Reset Password");
                CustomToast.show(getView(), "Failed to connect to server: " + t.getMessage(), false);
            }
        });
    }

    private void showOtpDialog(final String email, final String newPassword) {
        new OtpInputDialog(requireContext(), email, "forgot_pwd",
                verifiedCode -> resetPasswordOnBackend(email, verifiedCode, newPassword))
                .show();
    }


    private void resetPasswordOnBackend(final String email, final String code, final String newPassword) {
        resetPasswordButton.setEnabled(false);
        resetPasswordButton.setText("Updating password...");

        BrainScanApi api = RetrofitClient.getRetrofitInstance().create(BrainScanApi.class);
        api.resetPassword(email, code, newPassword).enqueue(new Callback<BaseResponse>() {
            @Override
            public void onResponse(Call<BaseResponse> call, Response<BaseResponse> response) {
                resetPasswordButton.setEnabled(true);
                resetPasswordButton.setText("Reset Password");

                if (response.isSuccessful() && response.body() != null && "success".equals(response.body().getStatus())) {
                    formContainer.setVisibility(View.GONE);
                    successContainer.setVisibility(View.VISIBLE);
                    successMessage.setText("Password reset successfully for " + email);
                } else {
                    String msg = (response.body() != null && response.body().getMessage() != null)
                            ? response.body().getMessage() : "Failed to reset password on the server";
                    CustomToast.show(getView(), msg, false);
                }
            }

            @Override
            public void onFailure(Call<BaseResponse> call, Throwable t) {
                resetPasswordButton.setEnabled(true);
                resetPasswordButton.setText("Reset Password");
                CustomToast.show(getView(), "Network error: " + t.getMessage(), false);
            }
        });
    }
}
