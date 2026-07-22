package com.example.brainhemorrhage;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import android.net.Uri;
import android.content.Context;
import android.content.SharedPreferences;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.ArrayList;
import java.util.Locale;
import com.example.brainhemorrhage.api.BaseResponse;
import com.example.brainhemorrhage.api.BrainScanApi;
import com.example.brainhemorrhage.api.RetrofitClient;
import java.io.File;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import java.io.InputStream;
import java.io.FileOutputStream;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.AnimatorSet;
import android.view.animation.OvershootInterpolator;
import android.view.animation.AccelerateDecelerateInterpolator;

public class ProcessingFragment extends Fragment {

    private ProgressBar progressBar;
    private TextView stageText, progressText;
    private LinearLayout detailsContainer;
    private Handler handler = new Handler();
    
    // Animation Views
    private View logoCard;
    private View scannerLine;
    private View glowRingOuter, glowRingInner;
    private AnimatorSet animatorSet;
    
    private String[] stages = {
        "Uploading scan...",
        "Assessing image quality...",
        "AI model processing...",
        "Analyzing brain regions...",
        "Generating report...",
        "Complete!"
    };
    
    private int[] progressSteps = {20, 40, 60, 80, 95, 100};
    private int currentStage = 0;

    private String patientId;
    private String patientName;
    private String patientAge;
    private String patientGender;
    private String patientNotes;
    private boolean isExistingPatient;
    private String imageUri;
    
    private YoloHelper.YoloResult tfliteResult = null;
    private boolean isError = false;
    private String inferenceErrorMsg = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_processing, container, false);

        progressBar = view.findViewById(R.id.progressBar);
        stageText = view.findViewById(R.id.stageText);
        progressText = view.findViewById(R.id.progressText);
        detailsContainer = view.findViewById(R.id.detailsContainer);

        // Animation elements
        logoCard = view.findViewById(R.id.logoCard);
        scannerLine = view.findViewById(R.id.scannerLine);
        glowRingOuter = view.findViewById(R.id.glowRingOuter);
        glowRingInner = view.findViewById(R.id.glowRingInner);

        startAnalyzingAnimations();

        // Get patient data
        Bundle args = getArguments();
        if (args != null) {
            patientId = args.getString("patientId");
            patientName = args.getString("patientName");
            patientAge = args.getString("patientAge");
            patientGender = args.getString("patientGender");
            patientNotes = args.getString("patientNotes");
            isExistingPatient = args.getBoolean("isExistingPatient", false);
            imageUri = args.getString("imageUri");
        }

        runInference();
        startProcessing();

        return view;
    }

    private void startAnalyzingAnimations() {
        if (logoCard == null || scannerLine == null || glowRingOuter == null || glowRingInner == null) {
            return;
        }

        float density = getResources().getDisplayMetrics().density;

        // 1. Logo Entry: Scale from collapsed (0f) to expanded (1f) with overshoot
        logoCard.setScaleX(0f);
        logoCard.setScaleY(0f);
        logoCard.setAlpha(0f);
        logoCard.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(800)
                .setInterpolator(new OvershootInterpolator(1.4f))
                .start();

        // 2. Laser Line sweeping animation inside the 100dp logoCard
        // logoCard is 100dp, laserLine is 4dp, so it sweeps 96dp
        float sweepDistance = 96f * density;
        ObjectAnimator laserAnim = ObjectAnimator.ofFloat(scannerLine, "translationY", 0f, sweepDistance);
        laserAnim.setDuration(1600);
        laserAnim.setRepeatMode(ValueAnimator.REVERSE);
        laserAnim.setRepeatCount(ValueAnimator.INFINITE);
        laserAnim.setInterpolator(new AccelerateDecelerateInterpolator());

        // 3. Staggered Breathing Glow Rings (Outer & Inner)
        ObjectAnimator glowOuterScaleX = ObjectAnimator.ofFloat(glowRingOuter, "scaleX", 0.9f, 1.1f);
        ObjectAnimator glowOuterScaleY = ObjectAnimator.ofFloat(glowRingOuter, "scaleY", 0.9f, 1.1f);
        ObjectAnimator glowOuterAlpha = ObjectAnimator.ofFloat(glowRingOuter, "alpha", 0.3f, 0.8f);

        glowOuterScaleX.setRepeatMode(ValueAnimator.REVERSE);
        glowOuterScaleX.setRepeatCount(ValueAnimator.INFINITE);
        glowOuterScaleY.setRepeatMode(ValueAnimator.REVERSE);
        glowOuterScaleY.setRepeatCount(ValueAnimator.INFINITE);
        glowOuterAlpha.setRepeatMode(ValueAnimator.REVERSE);
        glowOuterAlpha.setRepeatCount(ValueAnimator.INFINITE);

        ObjectAnimator glowInnerScaleX = ObjectAnimator.ofFloat(glowRingInner, "scaleX", 1.12f, 0.92f);
        ObjectAnimator glowInnerScaleY = ObjectAnimator.ofFloat(glowRingInner, "scaleY", 1.12f, 0.92f);
        ObjectAnimator glowInnerAlpha = ObjectAnimator.ofFloat(glowRingInner, "alpha", 0.7f, 0.2f);

        glowInnerScaleX.setRepeatMode(ValueAnimator.REVERSE);
        glowInnerScaleX.setRepeatCount(ValueAnimator.INFINITE);
        glowInnerScaleY.setRepeatMode(ValueAnimator.REVERSE);
        glowInnerScaleY.setRepeatCount(ValueAnimator.INFINITE);
        glowInnerAlpha.setRepeatMode(ValueAnimator.REVERSE);
        glowInnerAlpha.setRepeatCount(ValueAnimator.INFINITE);

        animatorSet = new AnimatorSet();
        animatorSet.playTogether(
            laserAnim,
            glowOuterScaleX, glowOuterScaleY, glowOuterAlpha,
            glowInnerScaleX, glowInnerScaleY, glowInnerAlpha
        );
        animatorSet.setDuration(2200);
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        animatorSet.start();
    }

    private void runInference() {
        if (imageUri == null) {
            isError = true;
            return;
        }
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                YoloHelper yoloHelper = new YoloHelper(requireContext());
                tfliteResult = yoloHelper.detect(requireContext(), Uri.parse(imageUri));
                yoloHelper.close();
            } catch (Exception e) {
                e.printStackTrace();
                isError = true;
                inferenceErrorMsg = e.getClass().getSimpleName() + ": " + e.getMessage();
            }
        });
    }

    private void startProcessing() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isAdded()) {
                    if (currentStage < stages.length) {
                        // If we are at the AI processing or analyzing stage, and inference isn't done, wait a bit
                        if ((currentStage == 2 || currentStage == 3 || currentStage == 4) && tfliteResult == null && !isError) {
                            handler.postDelayed(this, 1000); // Wait 1 more second
                            return;
                        }
                        updateProgress(currentStage);
                        currentStage++;
                        handler.postDelayed(this, 1500);
                    } else {

                            SharedPreferences prefs = requireActivity().getSharedPreferences("NuerocheckPrefs", Context.MODE_PRIVATE);
                            String safeDoctorEmail = prefs.getString("email", "Guest");
                            if (patientId == null || patientId.trim().isEmpty() || patientId.contains("@")) {
                                patientId = "PAT_" + (System.currentTimeMillis() % 100000000);
                            }
                            String safePatientId = patientId;

                            String result = "Normal";
                            String riskLevel = "LOW";
                            String tempImageUri = imageUri;

                            if (tfliteResult != null) {
                                if (tfliteResult.validationFailed) {
                                    result = "Invalid Image";
                                    riskLevel = "N/A";
                                } else {
                                     if (tfliteResult.hasHemorrhage) {
                                        result = "Hemorrhage";
                                        riskLevel = tfliteResult.highestConfidence > 0.7 ? "CRITICAL" : "HIGH";
                                     } else {
                                        result = "Normal";
                                        riskLevel = "LOW";
                                     }
                                    
                                    if (tfliteResult.processedImageUri != null) {
                                        tempImageUri = tfliteResult.processedImageUri.toString();
                                    }
                                }
                            } else if (isError) {
                                result = "Error";
                                riskLevel = "UNKNOWN";
                            }
                            
                            final String finalImageUri = tempImageUri;
                            final String finalResult = result;
                            final String finalRiskLevel = riskLevel;

                            // Ensure data is not null for DB safety
                            String safeName = (patientName != null && !patientName.isEmpty()) ? patientName : "Anonymous";
                            String safeAge = (patientAge != null && !patientAge.isEmpty()) ? patientAge : "0";
                            String safeGender = (patientGender != null && !patientGender.isEmpty()) ? patientGender : "Not Specified";

                            // Upload to backend using Retrofit
                            BrainScanApi api = RetrofitClient.getRetrofitInstance().create(BrainScanApi.class);
                            
                            RequestBody doctorEmailPart = RequestBody.create(MultipartBody.FORM, safeDoctorEmail);
                            RequestBody patientIdPart = safePatientId != null
                                    ? RequestBody.create(MultipartBody.FORM, safePatientId)
                                    : null;
                            RequestBody patientNamePart = RequestBody.create(MultipartBody.FORM, safeName);
                            RequestBody patientAgePart = RequestBody.create(MultipartBody.FORM, safeAge);
                            RequestBody patientGenderPart = RequestBody.create(MultipartBody.FORM, safeGender);
                            RequestBody resultPart = RequestBody.create(MultipartBody.FORM, finalResult);
                            RequestBody riskLevelPart = RequestBody.create(MultipartBody.FORM, finalRiskLevel);
                            RequestBody notesPart = RequestBody.create(MultipartBody.FORM, patientNotes != null ? patientNotes : "");

                            float iv = (tfliteResult != null) ? tfliteResult.intraventricular : 0f;
                            float ip = (tfliteResult != null) ? tfliteResult.intraparenchymal : 0f;
                            float sa = (tfliteResult != null) ? tfliteResult.subarachnoid : 0f;
                            float ep = (tfliteResult != null) ? tfliteResult.epidural : 0f;
                            float sd = (tfliteResult != null) ? tfliteResult.subdural : 0f;

                            RequestBody intraventricularPart = RequestBody.create(MultipartBody.FORM, String.valueOf(iv));
                            RequestBody intraparenchymalPart = RequestBody.create(MultipartBody.FORM, String.valueOf(ip));
                            RequestBody subarachnoidPart = RequestBody.create(MultipartBody.FORM, String.valueOf(sa));
                            RequestBody epiduralPart = RequestBody.create(MultipartBody.FORM, String.valueOf(ep));
                            RequestBody subduralPart = RequestBody.create(MultipartBody.FORM, String.valueOf(sd));
                            
                            MultipartBody.Part imagePart = null;
                            try {
                                if (finalImageUri != null) {
                                    Uri uri = Uri.parse(finalImageUri);
                                    File file = null;
                                    
                                    if ("file".equals(uri.getScheme())) {
                                        file = new File(uri.getPath());
                                    } else if ("content".equals(uri.getScheme())) {
                                        // Copy content to a temp file
                                        file = new File(requireContext().getCacheDir(), "upload_temp_" + System.currentTimeMillis() + ".jpg");
                                        try (InputStream is = requireContext().getContentResolver().openInputStream(uri);
                                             FileOutputStream fos = new FileOutputStream(file)) {
                                            byte[] buffer = new byte[4096];
                                            int read;
                                            while ((read = is.read(buffer)) != -1) {
                                                fos.write(buffer, 0, read);
                                            }
                                        }
                                    }
                                    
                                    if (file != null && file.exists()) {
                                        RequestBody requestFile = RequestBody.create(MediaType.parse("image/jpeg"), file);
                                        imagePart = MultipartBody.Part.createFormData("image", file.getName(), requestFile);
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            
                            Call<BaseResponse> call;
                            if (imagePart != null) {
                                call = api.uploadScan(doctorEmailPart, patientIdPart, patientNamePart, patientAgePart, patientGenderPart, resultPart, riskLevelPart,
                                        notesPart, null, intraventricularPart, intraparenchymalPart, subarachnoidPart, epiduralPart, subduralPart, imagePart);
                            } else {
                                call = api.uploadScan(doctorEmailPart, patientIdPart, patientNamePart, patientAgePart, patientGenderPart, resultPart, riskLevelPart,
                                        notesPart, null, intraventricularPart, intraparenchymalPart, subarachnoidPart, epiduralPart, subduralPart, null);
                            }
                            
                            call.enqueue(new Callback<BaseResponse>() {
                                @Override
                                public void onResponse(Call<BaseResponse> call, Response<BaseResponse> response) {
                                    if (response.isSuccessful() && response.body() != null && response.body().getPatient_id() != null) {
                                        // Update local patientId to the newly assigned authoritative DB primary key
                                        patientId = response.body().getPatient_id();
                                    }
                                    navigateNext(finalResult, finalImageUri);
                                }

                                @Override
                                public void onFailure(Call<BaseResponse> call, Throwable t) {
                                    // Navigate even if upload fails so user sees result
                                    navigateNext(finalResult, finalImageUri);
                                }
                            });
                    }
                }
            }
        }, 1500);
    }
    
    private void navigateNext(String result, String finalImageUri) {
        // Navigate to result screen
        if (isAdded()) {
            // Save locally in SQLite Database
            try {
                float intraventricular = 0f;
                float intraparenchymal = 0f;
                float subarachnoid = 0f;
                float epidural = 0f;
                float subdural = 0f;
                
                if (tfliteResult != null) {
                    intraventricular = tfliteResult.intraventricular;
                    intraparenchymal = tfliteResult.intraparenchymal;
                    subarachnoid = tfliteResult.subarachnoid;
                    epidural = tfliteResult.epidural;
                    subdural = tfliteResult.subdural;
                }
                
                java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
                java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault());
                java.util.Date now = new java.util.Date();
                String currentDate = dateFormat.format(now);
                String currentTime = timeFormat.format(now);
                
                DatabaseHelper.getInstance(requireContext()).insertScan(
                    patientId,
                    patientName,
                    patientAge,
                    patientGender,
                    currentDate,
                    currentTime,
                    result,
                    result != null && (result.toLowerCase().contains("abnormal") || result.toLowerCase().contains("hemorrhage")) ? (tfliteResult != null && tfliteResult.highestConfidence > 0.7f ? "CRITICAL" : "HIGH") : "LOW",
                    finalImageUri,
                    intraventricular,
                    intraparenchymal,
                    subarachnoid,
                    epidural,
                    subdural
                );
            } catch (Exception e) {
                e.printStackTrace();
            }

            View fragmentView = getView();
            if (fragmentView != null) {
                Bundle bundle = new Bundle();
                bundle.putString("patientId", patientId);
                bundle.putString("patientName", patientName);
                bundle.putString("patientAge", patientAge);
                bundle.putString("patientGender", patientGender);
                bundle.putString("notes", patientNotes);
                bundle.putBoolean("isExistingPatient", isExistingPatient);
                
                if (finalImageUri != null) {
                    bundle.putString("imageUri", finalImageUri);
                }
                
                // Pass result params so ResultFragment knows what to show
                bundle.putString("result", result);
                if (inferenceErrorMsg != null) {
                    bundle.putString("validationError", inferenceErrorMsg);
                } else if (tfliteResult != null && tfliteResult.validationFailed) {
                    bundle.putString("validationError", tfliteResult.validationError);
                }
                
                if (result != null && (result.toLowerCase().contains("abnormal") || result.toLowerCase().contains("hemorrhage"))) {
                    bundle.putString("scanId", "critical"); // Legacy support
                } else {
                    bundle.putString("scanId", "normal");
                }

                if (tfliteResult != null) {
                    bundle.putFloat("confidence", tfliteResult.highestConfidence);
                    bundle.putInt("detectionCount", tfliteResult.detectionCount);
                    bundle.putFloat("intraventricular", tfliteResult.intraventricular);
                    bundle.putFloat("intraparenchymal", tfliteResult.intraparenchymal);
                    bundle.putFloat("subarachnoid", tfliteResult.subarachnoid);
                    bundle.putFloat("epidural", tfliteResult.epidural);
                    bundle.putFloat("subdural", tfliteResult.subdural);
                }

                Navigation.findNavController(fragmentView)
                    .navigate(R.id.action_processing_to_result, bundle);
            }
        }
    }

    private void updateProgress(int stage) {
        int progress = progressSteps[stage];
        String stageLabel = stages[stage];

        progressBar.setProgress(progress);
        progressText.setText(progress + "%");
        stageText.setText(stageLabel);

        // Add completed stage to details
        TextView detailItem = new TextView(getContext());
        detailItem.setText(stageLabel);
        detailItem.setTextColor(ContextCompat.getColor(requireContext(), R.color.success_green));
        detailItem.setTextSize(14);
        detailItem.setPadding(0, 8, 0, 8);
        detailItem.setCompoundDrawablesWithIntrinsicBounds(
            R.drawable.ic_check_circle, 0, 0, 0);
        detailItem.setCompoundDrawablePadding(16);
        detailsContainer.addView(detailItem);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
        if (animatorSet != null) {
            animatorSet.cancel();
            animatorSet = null;
        }
    }
}
