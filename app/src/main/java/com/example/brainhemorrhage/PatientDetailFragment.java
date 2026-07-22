package com.example.brainhemorrhage;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.ArrayList;
import java.util.List;
import com.example.brainhemorrhage.api.BrainScanApi;
import com.example.brainhemorrhage.api.RetrofitClient;
import com.example.brainhemorrhage.api.ScanResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import android.content.Context;
import android.content.SharedPreferences;

public class PatientDetailFragment extends Fragment {

    private TextView patientInitial, patientNameText, patientIdText;
    private TextView patientAgeText, patientGenderText, totalScansText;
    private Button addNewScanButton;
    private RecyclerView scanTimelineRecyclerView;
    private ScanTimelineAdapter timelineAdapter;

    private String patientId;
    private String patientName;
    private String patientAge;
    private String patientGender;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_patient_detail, container, false);

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        initializeViews(view);
        loadPatientData();
        setupTimeline(view);
        setupClickListeners();

        return view;
    }

    private void initializeViews(View view) {
        patientInitial = view.findViewById(R.id.patientInitial);
        patientNameText = view.findViewById(R.id.patientNameText);
        patientIdText = view.findViewById(R.id.patientIdText);
        patientAgeText = view.findViewById(R.id.patientAgeText);
        patientGenderText = view.findViewById(R.id.patientGenderText);
        totalScansText = view.findViewById(R.id.totalScansText);
        addNewScanButton = view.findViewById(R.id.addNewScanButton);
        scanTimelineRecyclerView = view.findViewById(R.id.scanTimelineRecyclerView);
    }

    private void loadPatientData() {
        // Get patient data from bundle
        Bundle args = getArguments();
        if (args != null) {
            patientId = args.getString("patientId", "");
            patientName = args.getString("patientName", "Unknown Patient");
            patientAge = args.getString("patientAge", "--");
            patientGender = args.getString("patientGender", "--");
            
            patientAgeText.setText(patientAge);
            patientGenderText.setText(patientGender);
        } else {
            patientId = "";
            patientName = "Unknown Patient";
            patientAge = "--";
            patientGender = "--";
            patientAgeText.setText("--");
            patientGenderText.setText("--");
        }

        patientInitial.setText(patientName.length() > 0 ? patientName.substring(0, 1).toUpperCase() : "?");
        patientNameText.setText(patientName);
        patientIdText.setText(patientId);
    }

    private void setupTimeline(View rootView) {
        totalScansText.setText("Loading...");
        
        // Initialize empty adapter
        timelineAdapter = new ScanTimelineAdapter(new ArrayList<>(), scan -> {
            if (!isAdded()) return;
            Bundle args = new Bundle();
            args.putString("scanId", scan.getId());
            args.putString("patientId", patientId);
            args.putString("patientName", patientName);
            args.putString("patientAge", patientAge);
            args.putString("patientGender", patientGender);
            args.putString("result", scan.getResult());
            args.putBoolean("isExistingPatient", true);
            if (scan.getImageUri() != null) {
                args.putString("imageUri", scan.getImageUri());
            }
            Navigation.findNavController(rootView).navigate(R.id.action_patientDetail_to_result, args);
        });

        scanTimelineRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        scanTimelineRecyclerView.setAdapter(timelineAdapter);

        // Fetch data
        fetchPatientScans();
    }

    private void fetchPatientScans() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("NuerocheckPrefs", Context.MODE_PRIVATE);
        String doctorEmail = prefs.getString("email", "");

        // Determine how to filter:
        // If we have a numeric patient_id (from the patients table), use it as the sole filter —
        // this is the definitive identity and avoids mixing same-name patients.
        // Fall back to name + age only when patient_id is unavailable (legacy / new-patient path).
        final boolean hasPatientId = patientId != null && !patientId.isEmpty() && !patientId.contains("@");

        String queryPatientId     = hasPatientId ? patientId : null;
        String queryPatientName   = hasPatientId ? null : patientName;
        String queryPatientAge    = hasPatientId ? null : patientAge;
        String queryPatientGender = hasPatientId ? null : patientGender;

        BrainScanApi api = RetrofitClient.getRetrofitInstance().create(BrainScanApi.class);
        api.getPatientScans(doctorEmail, queryPatientId, queryPatientName, queryPatientAge, queryPatientGender)
                .enqueue(new Callback<ScanResponse>() {
            @Override
            public void onResponse(Call<ScanResponse> call, Response<ScanResponse> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null && "success".equals(response.body().getStatus())) {
                    List<ScanResponse.ScanItemDto> dtos = response.body().getData();
                    List<ScanTimelineItem> scans = new ArrayList<>();
                    
                    for (int i = 0; i < dtos.size(); i++) {
                        ScanResponse.ScanItemDto dto = dtos.get(i);
                        // No client-side age filter here — the backend already filtered by patient_id
                        // or name+age; extra filtering would incorrectly drop valid records.
                        ScanTimelineItem item = new ScanTimelineItem(
                                dto.getId(), dto.getDate_added(), dto.getTime_added(),
                                dto.getResult(), dto.getRisk_level(), i == dtos.size() - 1
                        );
                        item.setImageUri(dto.getImage_path());
                        scans.add(item);
                    }

                    if (scans.size() > 0) {
                        ScanResponse.ScanItemDto first = dtos.get(0);
                        // Update patientId to the authoritative DB value in case we arrived via name lookup
                        if (!hasPatientId && first.getPatient_id() != null) {
                            patientId = first.getPatient_id();
                        }
                        if (patientAge == null || patientAge.equals("--")) {
                            patientAge = first.getPatient_age();
                            patientAgeText.setText(patientAge != null ? patientAge : "--");
                        }
                        if (patientGender == null || patientGender.equals("--")) {
                            patientGender = first.getPatient_gender();
                        }
                        patientGenderText.setText(first.getPatient_gender() != null ? first.getPatient_gender() : "--");
                    }
                    
                    totalScansText.setText(String.valueOf(scans.size()));
                    if (timelineAdapter != null) timelineAdapter.updateData(scans);
                } else {
                    totalScansText.setText("0");
                }
            }

            @Override
            public void onFailure(Call<ScanResponse> call, Throwable t) {
                if (!isAdded()) return;
                try {
                    List<ScanTimelineItem> localTimeline = DatabaseHelper.getInstance(requireContext()).getScansForPatient(patientId);
                    totalScansText.setText(String.valueOf(localTimeline.size()));
                    if (timelineAdapter != null) timelineAdapter.updateData(localTimeline);
                } catch (Exception e) {
                    e.printStackTrace();
                    totalScansText.setText("Offline");
                }
            }
        });
    }

    /** Called from onResume – refreshes adapter data without recreating it. */
    private void refreshTimeline() {
        if (timelineAdapter == null || patientId == null) return;
        fetchPatientScans();
    }

    private void setupClickListeners() {
        addNewScanButton.setOnClickListener(v -> {
            // Navigate to scan instructions with patient data
            Bundle args = new Bundle();
            args.putString("patientId", patientId);
            args.putString("patientName", patientName);
            args.putString("patientAge", patientAgeText.getText().toString());
            args.putString("patientGender", patientGenderText.getText().toString());
            args.putBoolean("isExistingPatient", true);
            Navigation.findNavController(v).navigate(R.id.action_patientDetail_to_scanInstructions, args);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh timeline data without rebuilding the whole adapter
        refreshTimeline();
    }
}
