package com.example.brainhemorrhage;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.bumptech.glide.Glide;
import com.example.brainhemorrhage.api.BrainScanApi;
import com.example.brainhemorrhage.api.RetrofitClient;
import com.example.brainhemorrhage.api.ScanResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.ArrayList;
import java.util.List;

public class DashboardFragment extends Fragment {

    private TextView doctorNameText, totalCountText, normalCountText, abnormalCountText;
    private ImageView toolbarProfileImage;
    private RecyclerView recentScansRecyclerView;
    private BottomNavigationView bottomNavigation;
    private SharedPreferences prefs;

    private ScansAdapter scansAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);
        prefs = requireContext().getSharedPreferences("NuerocheckPrefs", Context.MODE_PRIVATE);

        initViews(view);
        loadProfile();
        setupRecentScans();
        setupClickListeners(view);
        setupBottomNavigation();

        // Apply premium touch/click bounce animations
        AnimationHelper.applyBouncePress(view.findViewById(R.id.newScanCard));
        AnimationHelper.applyBouncePress(view.findViewById(R.id.totalCard));
        AnimationHelper.applyBouncePress(view.findViewById(R.id.normalCard));
        AnimationHelper.applyBouncePress(view.findViewById(R.id.abnormalCard));
        AnimationHelper.applyBouncePress(view.findViewById(R.id.profileCard));

        // Staggered sequence slide-in entry animation
        AnimationHelper.animateViewsInSequence(
            view.findViewById(R.id.doctorNameText),
            view.findViewById(R.id.subtitleText),
            view.findViewById(R.id.newScanCard),
            view.findViewById(R.id.totalCard),
            view.findViewById(R.id.normalCard),
            view.findViewById(R.id.abnormalCard),
            view.findViewById(R.id.historyTitle),
            recentScansRecyclerView
        );

        // Continuous staggered floating animation on main cards
        float density = getResources().getDisplayMetrics().density;
        AnimationHelper.applyFloatingEffect(view.findViewById(R.id.newScanCard), 4f * density, 3000, 0);
        AnimationHelper.applyFloatingEffect(view.findViewById(R.id.totalCard), 4f * density, 3200, 200);
        AnimationHelper.applyFloatingEffect(view.findViewById(R.id.normalCard), 4f * density, 3400, 400);
        AnimationHelper.applyFloatingEffect(view.findViewById(R.id.abnormalCard), 4f * density, 3600, 600);

        return view;
    }

    private void initViews(View view) {
        doctorNameText          = view.findViewById(R.id.doctorNameText);
        toolbarProfileImage     = view.findViewById(R.id.toolbarProfileImage);
        recentScansRecyclerView = view.findViewById(R.id.recentScansRecyclerView);
        totalCountText          = view.findViewById(R.id.totalCountText);
        normalCountText         = view.findViewById(R.id.normalCountText);
        abnormalCountText       = view.findViewById(R.id.abnormalCountText);
        bottomNavigation        = view.findViewById(R.id.bottomNavigation);

        recentScansRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    private void loadProfile() {
        String name = prefs.getString("name", "Doctor");
        doctorNameText.setText("Hello, " + name);
        
        String photoUri = prefs.getString("profile_image", null);
        if (photoUri != null && !photoUri.isEmpty() && toolbarProfileImage != null) {
            String imageUrl = photoUri;
            if (!imageUrl.startsWith("http") && !imageUrl.startsWith("file") && !imageUrl.startsWith("content")) {
                imageUrl = RetrofitClient.BASE_URL + imageUrl;
            }
            toolbarProfileImage.setImageTintList(null); // Clear the white XML tint
            toolbarProfileImage.setPadding(0, 0, 0, 0); // Clear padding so photo fills the circle
            toolbarProfileImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.logo)
                .error(R.drawable.logo)
                .into(toolbarProfileImage);
        } else if (toolbarProfileImage != null) {
            toolbarProfileImage.setImageResource(R.drawable.ic_person);
            toolbarProfileImage.setImageTintList(androidx.core.content.ContextCompat.getColorStateList(requireContext(), android.R.color.white));
            float density = getResources().getDisplayMetrics().density;
            int padding = (int) (8 * density);
            toolbarProfileImage.setPadding(padding, padding, padding, padding);
            toolbarProfileImage.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        }
    }

    private void setupRecentScans() {
        scansAdapter = new ScansAdapter(new ArrayList<>(), scan -> {
            Bundle args = new Bundle();
            args.putString("patientId", scan.getDbPatientId());
            args.putString("patientName", scan.getPatientName());
            args.putString("patientAge", scan.getAge());
            args.putString("patientGender", scan.getGender());
            Navigation.findNavController(requireView()).navigate(R.id.action_dashboard_to_patientDetail, args);
        });

        recentScansRecyclerView.setAdapter(scansAdapter);
        fetchRecentScans();
    }

    private void fetchRecentScans() {
        String doctorEmail = prefs.getString("email", "");
        BrainScanApi api = RetrofitClient.getRetrofitInstance().create(BrainScanApi.class);
        api.getPatientScans(doctorEmail, null, null, null, null).enqueue(new Callback<ScanResponse>() {
            @Override
            public void onResponse(Call<ScanResponse> call, Response<ScanResponse> response) {
                if (response.isSuccessful() && response.body() != null && "success".equals(response.body().getStatus())) {
                    List<ScanResponse.ScanItemDto> dtos = response.body().getData();
                    int total = dtos.size();
                    int normal = 0;
                    int abnormal = 0;
                    
                    List<ScanItem> scans = new ArrayList<>();
                    for (int i = 0; i < dtos.size(); i++) {
                        ScanResponse.ScanItemDto dto = dtos.get(i);
                        String result = dto.getResult();
                        if (result != null && (result.toLowerCase().contains("abnormal") || result.toLowerCase().contains("hemorrhage"))) {
                            abnormal++;
                        } else if (result != null && result.toLowerCase().contains("normal")) {
                            normal++;
                        }
                        
                        if (i < 5) {
                            ScanItem item = new ScanItem(
                                Integer.parseInt(dto.getId()),
                                dto.getDoctor_email(),
                                dto.getPatient_name(),
                                result,
                                dto.getDate_added(),
                                dto.getImage_path()
                            );
                            item.setAge(dto.getPatient_age());
                            item.setGender(dto.getPatient_gender());
                            item.setDbPatientId(dto.getPatient_id());
                            scans.add(item);
                        }
                    }
                    
                    if (totalCountText != null) totalCountText.setText(String.valueOf(total));
                    if (normalCountText != null) normalCountText.setText(String.valueOf(normal));
                    if (abnormalCountText != null) abnormalCountText.setText(String.valueOf(abnormal));

                    if (scansAdapter != null) {
                        scansAdapter.updateData(scans);
                    }
                }
            }

            @Override
            public void onFailure(Call<ScanResponse> call, Throwable t) {
                try {
                    List<ScanItem> scans = DatabaseHelper.getInstance(requireContext()).getAllLocalPatients();
                    int total = scans.size();
                    int normal = 0;
                    int abnormal = 0;
                    
                    List<ScanItem> recentScans = new ArrayList<>();
                    for (int i = 0; i < scans.size(); i++) {
                        ScanItem item = scans.get(i);
                        String result = item.getResult();
                        if (result != null && (result.toLowerCase().contains("abnormal") || result.toLowerCase().contains("hemorrhage"))) {
                            abnormal++;
                        } else if (result != null && result.toLowerCase().contains("normal")) {
                            normal++;
                        }
                        if (i < 5) {
                            recentScans.add(item);
                        }
                    }
                    
                    if (totalCountText != null) totalCountText.setText(String.valueOf(total));
                    if (normalCountText != null) normalCountText.setText(String.valueOf(normal));
                    if (abnormalCountText != null) abnormalCountText.setText(String.valueOf(abnormal));

                    if (scansAdapter != null) {
                        scansAdapter.updateData(recentScans);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void setupClickListeners(View view) {
        view.findViewById(R.id.newScanCard).setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_dashboard_to_patientDetails));
        
        view.findViewById(R.id.totalCard).setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("filterType", "total");
            Navigation.findNavController(v).navigate(R.id.action_dashboard_to_history, args);
        });
            
        view.findViewById(R.id.normalCard).setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("filterType", "normal");
            Navigation.findNavController(v).navigate(R.id.action_dashboard_to_history, args);
        });
            
        view.findViewById(R.id.abnormalCard).setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("filterType", "abnormal");
            Navigation.findNavController(v).navigate(R.id.action_dashboard_to_history, args);
        });

        view.findViewById(R.id.viewAllText).setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("filterType", "total");
            Navigation.findNavController(v).navigate(R.id.action_dashboard_to_history, args);
        });
            
        view.findViewById(R.id.profileCard).setOnClickListener(v ->
            Navigation.findNavController(v).navigate(R.id.action_dashboard_to_settings));
    }

    private void setupBottomNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_dashboard);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_dashboard) return true;
            if (id == R.id.nav_scan) {
                Navigation.findNavController(requireView()).navigate(R.id.action_dashboard_to_patientDetails);
                return true;
            }
            if (id == R.id.nav_history) {
                Navigation.findNavController(requireView()).navigate(R.id.action_dashboard_to_history);
                return true;
            }
            if (id == R.id.nav_settings) {
                Navigation.findNavController(requireView()).navigate(R.id.action_dashboard_to_settings);
                return true;
            }
            return false;
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_dashboard);
        }
        fetchRecentScans();
    }
}
