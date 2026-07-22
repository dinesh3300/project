package com.example.brainhemorrhage;

import android.app.AlertDialog;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.brainhemorrhage.api.BaseResponse;
import com.example.brainhemorrhage.api.BrainScanApi;
import com.example.brainhemorrhage.api.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Reusable premium 6-box OTP dialog.
 *
 * Usage:
 *   new OtpInputDialog(requireContext(), email, "signup", (verifiedCode) -> {
 *       // proceed after OTP is verified
 *   }).show();
 */
public class OtpInputDialog {

    /** Callback invoked when the OTP has been successfully verified. */
    public interface OtpVerifiedCallback {
        void onVerified(String verifiedCode);
    }

    /** Optional callback used when the user taps "Resend" — caller can re-trigger sendOtp. */
    public interface ResendOtpCallback {
        void onResend(String email, String action, OtpInputDialog dialog);
    }

    private final Context context;
    private final String email;
    private final String action;
    private final OtpVerifiedCallback verifiedCallback;
    private ResendOtpCallback resendCallback;

    // Dialog views
    private AlertDialog dialog;
    private EditText[] boxes;
    private TextView subtitleText;
    private TextView errorText;
    private TextView timerText;
    private TextView resendText;
    private Button verifyButton;

    private CountDownTimer countDownTimer;
    private static final long RESEND_DELAY_MS = 60_000L;
    private boolean isVerifying = false;

    // ── Constructor ──────────────────────────────────────────────────────────────

    public OtpInputDialog(Context context, String email, String action,
                          OtpVerifiedCallback verifiedCallback) {
        this.context          = context;
        this.email            = email;
        this.action           = action;
        this.verifiedCallback = verifiedCallback;
    }

    public OtpInputDialog withResendCallback(ResendOtpCallback callback) {
        this.resendCallback = callback;
        return this;
    }

    // ── Public show() ────────────────────────────────────────────────────────────

    public void show() {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_otp_input, null);

        subtitleText = view.findViewById(R.id.otpSubtitleText);
        errorText    = view.findViewById(R.id.otpErrorText);
        timerText    = view.findViewById(R.id.otpTimerText);
        resendText   = view.findViewById(R.id.resendOtpText);
        verifyButton = view.findViewById(R.id.btnVerifyOtp);
        View cancelButton = view.findViewById(R.id.btnCancelOtp);

        subtitleText.setText("Enter the 6-digit code sent to\n" + email);

        boxes = new EditText[]{
            view.findViewById(R.id.otp1),
            view.findViewById(R.id.otp2),
            view.findViewById(R.id.otp3),
            view.findViewById(R.id.otp4),
            view.findViewById(R.id.otp5),
            view.findViewById(R.id.otp6)
        };

        setupBoxBehavior();
        startResendTimer();

        resendText.setOnClickListener(v -> {
            if (!resendText.isEnabled()) return;
            if (resendCallback != null) {
                resendCallback.onResend(email, action, this);
            } else {
                // Default: call sendOtp API and restart timer
                resendOtpDefault();
            }
        });

        verifyButton.setText(getVerifyButtonLabel());
        verifyButton.setOnClickListener(v -> attemptVerify());

        cancelButton.setOnClickListener(v -> {
            cancelTimer();
            dialog.dismiss();
        });

        dialog = new AlertDialog.Builder(context, R.style.OtpDialogTheme)
                .setView(view)
                .create();

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        // Focus first box and show keyboard
        boxes[0].requestFocus();
    }

    // ── Box behaviour: auto-advance & backspace ──────────────────────────────────

    private void setupBoxBehavior() {
        for (int i = 0; i < boxes.length; i++) {
            final int index = i;
            boxes[i].setOnFocusChangeListener((v, hasFocus) ->
                    updateBoxBackground((EditText) v, hasFocus));

            boxes[i].addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
                @Override public void afterTextChanged(Editable s) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    clearError();
                    updateBoxBackground(boxes[index], boxes[index].hasFocus());
                    if (s.length() == 1 && index < boxes.length - 1) {
                        boxes[index + 1].requestFocus();
                    }
                    // Auto-verify when all 6 boxes are filled
                    // Use a short delay so the last TextWatcher finishes
                    // before we fire the network call (avoids double-trigger)
                    if (allFilled()) {
                        boxes[index].postDelayed(() -> attemptVerify(), 150);
                    }
                }
            });

            // Handle backspace: move focus back
            boxes[i].setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_DEL
                        && event.getAction() == KeyEvent.ACTION_DOWN
                        && ((EditText) v).getText().toString().isEmpty()
                        && index > 0) {
                    boxes[index - 1].requestFocus();
                    boxes[index - 1].setText("");
                    return true;
                }
                return false;
            });
        }

        // Support paste: if user pastes 6 digits into the first box
        boxes[0].addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {
                if (s.length() == 6) {
                    String pasted = s.toString();
                    for (int i = 0; i < 6; i++) {
                        boxes[i].setText(String.valueOf(pasted.charAt(i)));
                    }
                    boxes[5].requestFocus();
                }
            }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });
    }

    private void updateBoxBackground(EditText box, boolean focused) {
        boolean filled = box.getText() != null && box.getText().length() > 0;
        if (filled) {
            box.setBackground(context.getResources().getDrawable(R.drawable.otp_box_filled, null));
            box.setTextColor(context.getResources().getColor(R.color.text_primary, null));
        } else if (focused) {
            box.setBackground(context.getResources().getDrawable(R.drawable.otp_box_focused, null));
            box.setTextColor(context.getResources().getColor(R.color.text_primary, null));
        } else {
            box.setBackground(context.getResources().getDrawable(R.drawable.otp_box_default, null));
            box.setTextColor(context.getResources().getColor(R.color.text_primary, null));
        }
    }

    // ── Resend timer ─────────────────────────────────────────────────────────────

    public void startResendTimer() {
        resendText.setEnabled(false);
        resendText.setAlpha(0.4f);

        cancelTimer();
        countDownTimer = new CountDownTimer(RESEND_DELAY_MS, 1000) {
            @Override public void onTick(long msRemaining) {
                timerText.setText("Resend available in " + (msRemaining / 1000) + "s");
            }
            @Override public void onFinish() {
                timerText.setText("Code expired. Resend now.");
                resendText.setEnabled(true);
                resendText.setAlpha(1f);
            }
        }.start();
    }

    private void cancelTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }

    // ── OTP verification ─────────────────────────────────────────────────────────

    private boolean allFilled() {
        for (EditText b : boxes) {
            if (b.getText() == null || b.getText().toString().isEmpty()) return false;
        }
        return true;
    }

    private String getCode() {
        StringBuilder sb = new StringBuilder();
        for (EditText b : boxes) sb.append(b.getText().toString());
        return sb.toString();
    }

    private void attemptVerify() {
        if (isVerifying) return;
        String code = getCode();
        if (code.length() != 6) {
            showError("Please fill all 6 digits.");
            return;
        }

        isVerifying = true;
        setLoading(true);

        BrainScanApi api = RetrofitClient.getRetrofitInstance().create(BrainScanApi.class);
        api.verifyOtp(email, code, action).enqueue(new Callback<BaseResponse>() {
            @Override
            public void onResponse(Call<BaseResponse> call, Response<BaseResponse> response) {
                isVerifying = false;
                setLoading(false);
                if (response.isSuccessful() && response.body() != null
                        && "success".equals(response.body().getStatus())) {
                    cancelTimer();
                    dialog.dismiss();
                    if (verifiedCallback != null) verifiedCallback.onVerified(code);
                } else {
                    String msg = (response.body() != null && response.body().getMessage() != null)
                            ? response.body().getMessage() : "Invalid or expired OTP.";
                    showError(msg);
                    shakeBoxes();
                }
            }

            @Override
            public void onFailure(Call<BaseResponse> call, Throwable t) {
                isVerifying = false;
                setLoading(false);
                showError("Network error. Please try again.");
            }
        });
    }

    private void resendOtpDefault() {
        resendText.setEnabled(false);
        resendText.setAlpha(0.4f);
        timerText.setText("Sending new code…");
        clearBoxes();

        BrainScanApi api = RetrofitClient.getRetrofitInstance().create(BrainScanApi.class);
        api.sendOtp(email, action).enqueue(new Callback<BaseResponse>() {
            @Override
            public void onResponse(Call<BaseResponse> call, Response<BaseResponse> response) {
                if (response.isSuccessful() && response.body() != null
                        && "success".equals(response.body().getStatus())) {
                    CustomToast.show(context, "New code sent to " + email, true);
                    startResendTimer();
                    boxes[0].requestFocus();
                } else {
                    String msg = response.body() != null ? response.body().getMessage() : "Failed to resend verification code.";
                    ErrorDialogHelper.show(context, "Resend Failed", msg);
                    resendText.setEnabled(true);
                    resendText.setAlpha(1f);
                    timerText.setText("Code expired. Resend now.");
                }
            }

            @Override
            public void onFailure(Call<BaseResponse> call, Throwable t) {
                ErrorDialogHelper.show(context, "Connection Error", "Failed to connect to server: " + t.getLocalizedMessage());
                resendText.setEnabled(true);
                resendText.setAlpha(1f);
                timerText.setText("Code expired. Resend now.");
            }
        });
    }

    // ── UI helpers ────────────────────────────────────────────────────────────────

    private void setLoading(boolean loading) {
        if (verifyButton != null) {
            verifyButton.setEnabled(!loading);
            verifyButton.setText(loading ? "Verifying…" : getVerifyButtonLabel());
        }
        for (EditText b : boxes) b.setEnabled(!loading);
    }

    private void showError(String msg) {
        errorText.setText(msg);
        errorText.setVisibility(View.VISIBLE);
    }

    private void clearError() {
        errorText.setVisibility(View.GONE);
    }

    private void clearBoxes() {
        for (int i = 0; i < boxes.length; i++) {
            boxes[i].setText("");
            updateBoxBackground(boxes[i], i == 0);
        }
        boxes[0].requestFocus();
    }

    /** Brief scale-pulse to signal wrong OTP entry */
    private void shakeBoxes() {
        for (EditText b : boxes) {
            b.animate()
                .scaleX(1.08f).scaleY(1.08f).setDuration(80)
                .withEndAction(() -> b.animate().scaleX(1f).scaleY(1f).setDuration(80).start())
                .start();
        }
    }

    private String getVerifyButtonLabel() {
        switch (action) {
            case "signup":     return "Verify & Register";
            case "forgot_pwd": return "Verify & Reset";
            case "update_pwd": return "Verify & Update";
            default:           return "Verify";
        }
    }
}
