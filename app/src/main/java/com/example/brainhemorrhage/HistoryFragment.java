package com.example.brainhemorrhage;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import androidx.appcompat.widget.Toolbar;

public class HistoryFragment extends Fragment {

    private String filterType = "total";

    private RecyclerView patientsRecyclerView;
    private ScansAdapter patientsAdapter;
    private View shimmerLayout;
    private View emptyStateLayout;
    private View errorStateLayout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        if (getArguments() != null) {
            filterType = getArguments().getString("filterType", "total");
        }

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        if (toolbar != null) {
            if ("normal".equalsIgnoreCase(filterType)) {
                toolbar.setTitle("Normal Scans");
            } else if ("abnormal".equalsIgnoreCase(filterType)) {
                toolbar.setTitle("Abnormal Scans");
            } else {
                toolbar.setTitle("Patient Records");
            }
            toolbar.setNavigationIcon(R.drawable.ic_back);
            toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(v).navigateUp());
        }

        patientsRecyclerView = view.findViewById(R.id.patientsRecyclerView);
        shimmerLayout = view.findViewById(R.id.shimmerLayout);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
        errorStateLayout = view.findViewById(R.id.errorStateLayout);

        view.findViewById(R.id.retryButton).setOnClickListener(v -> fetchPatientsFromBackend());

        setupPatientsList();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh so a newly-added scan shows up when returning from the processing flow
        if (patientsAdapter != null) {
            fetchPatientsFromBackend();
        }
    }

    private void setupPatientsList() {
        patientsAdapter = new ScansAdapter(new ArrayList<>(), item -> {
            // ... (keep navigation logic)
            if ("normal".equalsIgnoreCase(filterType) || "abnormal".equalsIgnoreCase(filterType)) {
                Bundle args = new Bundle();
                args.putString("scanId", String.valueOf(item.getId()));
                args.putString("patientId", item.getDbPatientId());
                args.putString("patientName", item.getPatientName());
                args.putString("patientAge", item.getAge());
                args.putString("patientGender", item.getGender());
                args.putString("result", item.getResult());
                args.putBoolean("isExistingPatient", true);
                if (item.getImagePath() != null) {
                    args.putString("imageUri", item.getImagePath());
                }
                View fragmentView = getView();
                if (fragmentView != null) {
                    Navigation.findNavController(fragmentView).navigate(R.id.action_history_to_result, args);
                }
            } else {
                Bundle args = new Bundle();
                args.putString("patientId", item.getDbPatientId());
                args.putString("patientName", item.getPatientName());
                args.putString("patientAge", item.getAge());
                args.putString("patientGender", item.getGender());
                View fragmentView = getView();
                if (fragmentView != null) {
                    Navigation.findNavController(fragmentView).navigate(R.id.action_history_to_patientDetail, args);
                }
            }
        });


        patientsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        patientsRecyclerView.setAdapter(patientsAdapter);

        fetchPatientsFromBackend();
    }

    private void showLoading(boolean show) {
        if (shimmerLayout != null) {
            shimmerLayout.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (patientsRecyclerView != null) {
            patientsRecyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
        if (emptyStateLayout != null) emptyStateLayout.setVisibility(View.GONE);
        if (errorStateLayout != null) errorStateLayout.setVisibility(View.GONE);
    }

    private void fetchPatientsFromBackend() {
        showLoading(true);
        SharedPreferences prefs = requireActivity().getSharedPreferences("NuerocheckPrefs", Context.MODE_PRIVATE);
        String doctorEmail = prefs.getString("email", "");

        com.example.brainhemorrhage.api.BrainScanApi api = com.example.brainhemorrhage.api.RetrofitClient.getRetrofitInstance().create(com.example.brainhemorrhage.api.BrainScanApi.class);
        api.getPatientScans(doctorEmail, null, null, null, null).enqueue(new retrofit2.Callback<com.example.brainhemorrhage.api.ScanResponse>() {
            @Override
            public void onResponse(retrofit2.Call<com.example.brainhemorrhage.api.ScanResponse> call, retrofit2.Response<com.example.brainhemorrhage.api.ScanResponse> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null && "success".equals(response.body().getStatus())) {
                    List<com.example.brainhemorrhage.api.ScanResponse.ScanItemDto> dtos = response.body().getData();
                    List<ScanItem> displayList = new ArrayList<>();
                    
                    // ... (keep filter logic)
                    if ("normal".equalsIgnoreCase(filterType)) {
                        for (com.example.brainhemorrhage.api.ScanResponse.ScanItemDto dto : dtos) {
                            String res = dto.getResult();
                            if (res != null && res.toLowerCase().contains("normal") && !res.toLowerCase().contains("abnormal") && !res.toLowerCase().contains("hemorrhage")) {
                                ScanItem item = new ScanItem(
                                    Integer.parseInt(dto.getId()), dto.getDoctor_email(), dto.getPatient_name(),
                                    dto.getResult(), dto.getDate_added(), dto.getImage_path()
                                );
                                item.setAge(dto.getPatient_age());
                                item.setGender(dto.getPatient_gender());
                                item.setDbPatientId(dto.getPatient_id());
                                displayList.add(item);
                            }
                        }
                    } else if ("abnormal".equalsIgnoreCase(filterType)) {
                        for (com.example.brainhemorrhage.api.ScanResponse.ScanItemDto dto : dtos) {
                            String res = dto.getResult();
                            if (res != null && (res.toLowerCase().contains("abnormal") || res.toLowerCase().contains("hemorrhage"))) {
                                ScanItem item = new ScanItem(
                                    Integer.parseInt(dto.getId()), dto.getDoctor_email(), dto.getPatient_name(),
                                    dto.getResult(), dto.getDate_added(), dto.getImage_path()
                                );
                                item.setAge(dto.getPatient_age());
                                item.setGender(dto.getPatient_gender());
                                item.setDbPatientId(dto.getPatient_id());
                                displayList.add(item);
                            }
                        }
                    } else {
                        // Group scans by patient_id to show a unique list of patients
                        java.util.Map<String, ScanItem> patientMap = new java.util.LinkedHashMap<>();
                        for (com.example.brainhemorrhage.api.ScanResponse.ScanItemDto dto : dtos) {
                            String patientKey = dto.getPatient_id();
                            if (patientKey == null || patientKey.isEmpty()) {
                                patientKey = dto.getPatient_name() + "\u0000"
                                           + dto.getPatient_age()  + "\u0000"
                                           + dto.getPatient_gender();
                            }
                            if (!patientMap.containsKey(patientKey)) {
                                ScanItem item = new ScanItem(
                                    Integer.parseInt(dto.getId()),
                                    dto.getDoctor_email(),
                                    dto.getPatient_name(),
                                    dto.getResult(),
                                    dto.getDate_added(),
                                    dto.getImage_path()
                                );
                                item.setAge(dto.getPatient_age());
                                item.setGender(dto.getPatient_gender());
                                item.setDbPatientId(dto.getPatient_id());
                                patientMap.put(patientKey, item);
                            }
                        }
                        displayList.addAll(patientMap.values());
                    }
                    
                    if (displayList.isEmpty()) {
                        if (emptyStateLayout != null) emptyStateLayout.setVisibility(View.VISIBLE);
                        if (patientsRecyclerView != null) patientsRecyclerView.setVisibility(View.GONE);
                    } else if (patientsAdapter != null) {
                        patientsAdapter.updateData(displayList);
                    }
                } else {
                    if (errorStateLayout != null) errorStateLayout.setVisibility(View.VISIBLE);
                    if (patientsRecyclerView != null) patientsRecyclerView.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(retrofit2.Call<com.example.brainhemorrhage.api.ScanResponse> call, Throwable t) {
                showLoading(false);
                try {
                    List<ScanItem> localScans = DatabaseHelper.getInstance(requireContext()).getAllLocalPatients();
                    List<ScanItem> displayList = new ArrayList<>();
                    
                    // ... (keep filter logic)
                    if ("normal".equalsIgnoreCase(filterType)) {
                        for (ScanItem item : localScans) {
                            String res = item.getResult();
                            if (res != null && res.toLowerCase().contains("normal") && !res.toLowerCase().contains("abnormal") && !res.toLowerCase().contains("hemorrhage")) {
                                displayList.add(item);
                            }
                        }
                    } else if ("abnormal".equalsIgnoreCase(filterType)) {
                        for (ScanItem item : localScans) {
                            String res = item.getResult();
                            if (res != null && (res.toLowerCase().contains("abnormal") || res.toLowerCase().contains("hemorrhage"))) {
                                displayList.add(item);
                            }
                        }
                    } else {
                        displayList.addAll(localScans);
                    }
                    
                    if (displayList.isEmpty()) {
                        if (errorStateLayout != null) errorStateLayout.setVisibility(View.VISIBLE);
                        if (patientsRecyclerView != null) patientsRecyclerView.setVisibility(View.GONE);
                    } else if (patientsAdapter != null) {
                        patientsAdapter.updateData(displayList);
                    }
                } catch (Exception e) {
                    if (errorStateLayout != null) errorStateLayout.setVisibility(View.VISIBLE);
                    if (patientsRecyclerView != null) patientsRecyclerView.setVisibility(View.GONE);
                }
            }
        });
    }
}
