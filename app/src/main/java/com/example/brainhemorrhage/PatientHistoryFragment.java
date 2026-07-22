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
import java.util.ArrayList;
import java.util.List;

public class PatientHistoryFragment extends Fragment {

    private String patientName;
    private RecyclerView scansRecyclerView;
    private Button newScanButton;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            patientName = getArguments().getString("patientName", "Unknown Patient");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_patient_history, container, false);

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(v).navigateUp());
        toolbar.setTitle(patientName);

        TextView patientNameText = view.findViewById(R.id.patientNameDetail);
        TextView patientInfoText = view.findViewById(R.id.patientInfoDetail);
        newScanButton = view.findViewById(R.id.newScanForPatientButton);
        scansRecyclerView = view.findViewById(R.id.patientScansRecyclerView);

        patientNameText.setText(patientName);
        // Obsolete fragment: Real patient history is handled by PatientDetailFragment.
        patientInfoText.setText("No data available");

        setupScansList();
        
        newScanButton.setOnClickListener(v -> {
            // Disabled in obsolete fragment path
        });

        return view;
    }

    private void setupScansList() {
        // Clear out fake scan data completely
        List<ScanItem> scans = new ArrayList<>();
        ScansAdapter adapter = new ScansAdapter(scans, scan -> {});
        scansRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        scansRecyclerView.setAdapter(adapter);
    }
}
