package com.example.brainhemorrhage;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.net.Uri;
import com.bumptech.glide.Glide;
import com.example.brainhemorrhage.api.RetrofitClient;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ResultFragment extends Fragment {

    private View resultIconBackground;
    private ImageView resultIcon;
    private TextView resultStatusText;
    private TextView patientNameText, scanDateText;
    private LinearLayout findingsContainer, subtypeContainer;
    private View subtypeTitle, subtypeCard;

    private Button shareReportButton, backToDashboardButton, viewHistoryButton;
    private View scanImageCard;
    private ImageView scanImageView;

    private boolean isAbnormal = false;
    private String patientId;
    private String patientName;
    private String patientAge;
    private String patientGender;
    private boolean isExistingPatient;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_result, container, false);

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> {
            if (isExistingPatient && patientId != null) {
                Bundle navArgs = new Bundle();
                navArgs.putString("patientId", patientId);
                navArgs.putString("patientName", patientName);
                navArgs.putString("patientAge", patientAge);
                navArgs.putString("patientGender", patientGender);
                Navigation.findNavController(v).navigate(R.id.action_result_to_patientDetail, navArgs);
            } else {
                Navigation.findNavController(v).navigateUp();
            }
        });

        // Get patient data
        Bundle args = getArguments();
        if (args != null) {
            patientId = args.getString("patientId");
            patientName = args.getString("patientName");
            patientAge = args.getString("patientAge");
            patientGender = args.getString("patientGender");
            isExistingPatient = args.getBoolean("isExistingPatient", false);
        }

        if (patientName == null || patientName.isEmpty()) {
            android.content.SharedPreferences prefs = requireContext().getSharedPreferences("NuerocheckPrefs", android.content.Context.MODE_PRIVATE);
            patientName = prefs.getString("name", "Doctor");
        }

        initializeViews(view);
        loadResultData(view);

        setupClickListeners(view);

        // Connect button touch/bounce scaling
        AnimationHelper.applyBouncePress(shareReportButton);
        AnimationHelper.applyBouncePress(backToDashboardButton);
        AnimationHelper.applyBouncePress(viewHistoryButton);

        // Cascade/stagger slide entrance for all result sections
        AnimationHelper.animateViewsInSequence(
            view.findViewById(R.id.resultHeaderCard),
            scanImageCard,
            view.findViewById(R.id.patientDetailsCard),
            findingsContainer,
            shareReportButton,
            backToDashboardButton,
            viewHistoryButton
        );

        return view;
    }

    private void initializeViews(View view) {
        resultIconBackground = view.findViewById(R.id.resultIconBackground);
        resultIcon = view.findViewById(R.id.resultIcon);
        resultStatusText = view.findViewById(R.id.resultStatusText);
        patientNameText = view.findViewById(R.id.patientNameText);
        scanDateText = view.findViewById(R.id.scanDateText);
        findingsContainer = view.findViewById(R.id.findingsContainer);
        subtypeContainer = view.findViewById(R.id.subtypeContainer);
        subtypeTitle = view.findViewById(R.id.subtypeTitle);
        subtypeCard = view.findViewById(R.id.subtypeCard);

        shareReportButton = view.findViewById(R.id.shareReportButton);
        backToDashboardButton = view.findViewById(R.id.backToDashboardButton);
        viewHistoryButton = view.findViewById(R.id.viewHistoryButton);
        scanImageCard = view.findViewById(R.id.scanImageCard);
        scanImageView = view.findViewById(R.id.scanImageView);
    }

    private void loadResultData(View view) {
        Bundle args = getArguments();

        if (args != null) {
            String scanId = args.getString("scanId");
            
            // 1. Fetch complete details from SQLite if navigating from history/timeline (numeric scanId)
            if (scanId != null && scanId.matches("\\d+")) {
                int dbId = Integer.parseInt(scanId);
                try {
                    android.database.Cursor cursor = DatabaseHelper.getInstance(requireContext()).getScanById(dbId);
                    if (cursor != null && cursor.moveToFirst()) {
                        String dbResult = cursor.getString(cursor.getColumnIndexOrThrow("result"));
                        String dbRiskLevel = cursor.getString(cursor.getColumnIndexOrThrow("risk_level"));
                        String dbImageUri = cursor.getString(cursor.getColumnIndexOrThrow("image_uri"));
                        String dbPatientId = cursor.getString(cursor.getColumnIndexOrThrow("patient_id"));
                        String dbPatientName = cursor.getString(cursor.getColumnIndexOrThrow("patient_name"));
                        String dbPatientAge = cursor.getString(cursor.getColumnIndexOrThrow("patient_age"));
                        String dbPatientGender = cursor.getString(cursor.getColumnIndexOrThrow("patient_gender"));
                        
                        float intraventricular = cursor.getFloat(cursor.getColumnIndexOrThrow("intraventricular"));
                        float intraparenchymal = cursor.getFloat(cursor.getColumnIndexOrThrow("intraparenchymal"));
                        float subarachnoid = cursor.getFloat(cursor.getColumnIndexOrThrow("subarachnoid"));
                        float epidural = cursor.getFloat(cursor.getColumnIndexOrThrow("epidural"));
                        float subdural = cursor.getFloat(cursor.getColumnIndexOrThrow("subdural"));
                        
                        // Save back to args so subsequent rendering logic consumes the DB source of truth
                        args.putString("result", dbResult);
                        args.putString("imageUri", dbImageUri);
                        args.putString("patientId", dbPatientId);
                        args.putString("patientName", dbPatientName);
                        args.putString("patientAge", dbPatientAge);
                        args.putString("patientGender", dbPatientGender);
                        
                        args.putFloat("intraventricular", intraventricular);
                        args.putFloat("intraparenchymal", intraparenchymal);
                        args.putFloat("subarachnoid", subarachnoid);
                        args.putFloat("epidural", epidural);
                        args.putFloat("subdural", subdural);
                        
                        // Deduce highest confidence or default for older/abnormal records
                        float maxProb = Math.max(intraventricular, Math.max(intraparenchymal, Math.max(subarachnoid, Math.max(epidural, subdural))));
                        args.putFloat("confidence", maxProb > 0.0f ? maxProb : (("abnormal".equalsIgnoreCase(dbResult) || "hemorrhage".equalsIgnoreCase(dbResult)) ? 0.85f : 0.0f));
                        args.putInt("detectionCount", ("abnormal".equalsIgnoreCase(dbResult) || "hemorrhage".equalsIgnoreCase(dbResult)) ? 1 : 0);
                        
                        // Sync class members
                        patientId = dbPatientId;
                        patientName = dbPatientName;
                        patientAge = dbPatientAge;
                        patientGender = dbPatientGender;
                        
                        cursor.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // Determine result based on scanId or other markers
            String resultStr = args.getString("result");
            isAbnormal = (scanId != null && (scanId.contains("critical") || scanId.equalsIgnoreCase("Abnormal") || scanId.equalsIgnoreCase("Hemorrhage"))) ||
                         (resultStr != null && (resultStr.toLowerCase().contains("abnormal") || resultStr.toLowerCase().contains("hemorrhage")));

            String imageUriString = args.getString("imageUri");
            if (imageUriString != null && !imageUriString.isEmpty()) {
                scanImageCard.setVisibility(View.VISIBLE);
                
                // Prepend BASE_URL if it's a relative path
                if (!imageUriString.startsWith("http") && !imageUriString.startsWith("file") && !imageUriString.startsWith("content")) {
                    imageUriString = RetrofitClient.BASE_URL + imageUriString;
                }

                Glide.with(this)
                    .load(imageUriString)
                    .placeholder(R.drawable.logo)
                    .error(R.drawable.logo)
                    .fitCenter()
                    .into(scanImageView);
            }
            
            // Also update patient details if passed
            String age = args.getString("patientAge");
            String gender = args.getString("patientGender");
            if (age != null || gender != null) {
                TextView ageGenderText = view.findViewById(R.id.patientAgeGenderText);
                if (ageGenderText != null) {
                    ageGenderText.setText((age != null ? age : "--") + " / " + (gender != null ? gender : "--"));
                }
            }

        }



        if (isAbnormal) {
            resultIconBackground.setBackgroundTintList(
                ContextCompat.getColorStateList(requireContext(), R.color.danger_red_10));
            resultIcon.setImageResource(R.drawable.ic_warning);
            resultIcon.setImageTintList(
                ContextCompat.getColorStateList(requireContext(), R.color.danger_red));
            
            resultStatusText.setText("HEMORRHAGE DETECTED");
            resultStatusText.setTextColor(ContextCompat.getColor(requireContext(), R.color.danger_red));

            addFinding("AI MODEL PREDICTIONS:");
            addFinding("- Result: Brain Hemorrhage Detected");

            addRecommendation("IMMEDIATE: Consult a neurologist or neurosurgeon", true);
            addRecommendation("Schedule a follow-up head CT scan for detailed assessment", false);
            addRecommendation("Monitor patient closely for consciousness changes", false);
        } else if ("Error".equalsIgnoreCase(args.getString("result"))) {
            resultIconBackground.setBackgroundTintList(
                ContextCompat.getColorStateList(requireContext(), R.color.danger_red_10));
            resultIcon.setImageResource(R.drawable.ic_warning);
            resultIcon.setImageTintList(
                ContextCompat.getColorStateList(requireContext(), R.color.danger_red));
            
            resultStatusText.setText("ERROR");
            
            addFinding("- AI model failed to process the image");
            addFinding("- Please ensure the image is a clear brain scan");
            addFinding("- Try uploading the image again");
        } else if ("Invalid Image".equalsIgnoreCase(args.getString("result"))) {
            resultIconBackground.setBackgroundTintList(
                ContextCompat.getColorStateList(requireContext(), R.color.danger_red_10));
            resultIcon.setImageResource(R.drawable.ic_warning);
            resultIcon.setImageTintList(
                ContextCompat.getColorStateList(requireContext(), R.color.danger_red));
            
            resultStatusText.setText("INVALID IMAGE");
            
            addFinding("- Image validation failed");
            addFinding("- Input rejected: not a valid brain image");
        } else {
            resultIconBackground.setBackgroundTintList(
                ContextCompat.getColorStateList(requireContext(), R.color.success_green_10));
            resultIcon.setImageResource(R.drawable.ic_check_circle);
            resultIcon.setImageTintList(
                ContextCompat.getColorStateList(requireContext(), R.color.success_green));
            
            resultStatusText.setText("HEMORRHAGE NOT DETECTED");
            resultStatusText.setTextColor(ContextCompat.getColor(requireContext(), R.color.success_green));

            addFinding("AI MODEL PREDICTIONS:");
            addFinding("- Result: No Brain Hemorrhage Detected");

            addRecommendation("No immediate intervention required", false);
            addRecommendation("Follow standard screening protocols", false);
        }

        String notes = args.getString("notes");
        if (notes != null && !notes.isEmpty()) {
            addFinding("\nCLINICAL NOTES:");
            addFinding(notes);
        }

        if (isAbnormal) {
            subtypeTitle.setVisibility(View.VISIBLE);
            subtypeCard.setVisibility(View.VISIBLE);
            addSubtypeResult("Intraventricular", args.getFloat("intraventricular", 0f));
            addSubtypeResult("Intraparenchymal", args.getFloat("intraparenchymal", 0f));
            addSubtypeResult("Subarachnoid", args.getFloat("subarachnoid", 0f));
            addSubtypeResult("Epidural", args.getFloat("epidural", 0f));
            addSubtypeResult("Subdural", args.getFloat("subdural", 0f));
        }

        patientNameText.setText(patientName != null ? patientName : "N/A");
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        scanDateText.setText(dateFormat.format(new Date()));
    }

    private void addSubtypeResult(String label, float probability) {
        View row = getLayoutInflater().inflate(R.layout.item_subtype_probability, subtypeContainer, false);
        TextView labelTv = row.findViewById(R.id.subtypeLabel);
        TextView percentTv = row.findViewById(R.id.subtypePercent);
        ProgressBar progress = row.findViewById(R.id.subtypeProgress);

        labelTv.setText(label);
        int pct = (int) (probability * 100);
        percentTv.setText(pct + "%");
        progress.setProgress(pct);

        if (probability >= 0.5f) {
            progress.setProgressTintList(android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.primary)));
        }

        subtypeContainer.addView(row);
    }

    private void addFinding(String text) {
        TextView tv = new TextView(getContext());
        tv.setText(text);
        tv.setTextSize(14);
        tv.setPadding(0, 8, 0, 8);
        findingsContainer.addView(tv);
    }

    private void addRecommendation(String text, boolean isCritical) {
        TextView tv = new TextView(getContext());
        tv.setText(text);
        tv.setTextSize(14);
        tv.setPadding(0, 8, 0, 8);
        if (isCritical) {
            tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.danger_red));
            tv.setTypeface(null, android.graphics.Typeface.BOLD);
        }
        findingsContainer.addView(tv); // Add to findings instead since recommendations was removed
    }


    private void setupClickListeners(View view) {
        shareReportButton.setOnClickListener(v -> {
            // Share functionality placeholder
        });

        backToDashboardButton.setOnClickListener(v -> {
            if (isExistingPatient && patientId != null) {
                Bundle navArgs = new Bundle();
                navArgs.putString("patientId", patientId);
                navArgs.putString("patientName", patientName);
                navArgs.putString("patientAge", patientAge);
                navArgs.putString("patientGender", patientGender);
                Navigation.findNavController(view).navigate(R.id.action_result_to_patientDetail, navArgs);
            } else {
                Navigation.findNavController(view).navigate(R.id.action_result_to_dashboard);
            }
        });

        // Explicit "View History" button — always navigates to history
        viewHistoryButton.setOnClickListener(v ->
            Navigation.findNavController(view).navigate(R.id.action_result_to_history));
    }
}
