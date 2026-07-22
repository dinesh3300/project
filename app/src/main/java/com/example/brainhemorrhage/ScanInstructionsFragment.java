package com.example.brainhemorrhage;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

public class ScanInstructionsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_scan_instructions, container, false);

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        Button continueButton = view.findViewById(R.id.continueButton);

        continueButton.setOnClickListener(v -> {
            Bundle args = getArguments();
            if (args != null && args.getBoolean("isExistingPatient", false)) {
                Navigation.findNavController(v).navigate(R.id.action_scanInstructions_to_upload, args);
            } else {
                Navigation.findNavController(v).navigate(R.id.action_scanInstructions_to_patientDetails, args);
            }
        });

        return view;
    }
}
