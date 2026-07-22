package com.example.brainhemorrhage;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

public class ContactSupportFragment extends Fragment {

    private AutoCompleteTextView categoryDropdown;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_contact_support, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        categoryDropdown = view.findViewById(R.id.categoryDropdown);
        String[] categories = {"Technical Issue", "Account Issue", "Scan Processing Issue", "Billing Issue", "Feedback", "Feature Request", "Other"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, categories);
        categoryDropdown.setAdapter(adapter);

        if (getArguments() != null) {
            String category = getArguments().getString("category");
            if (category != null) categoryDropdown.setText(category, false);
        }

        MaterialButton submitButton = view.findViewById(R.id.submitButton);
        submitButton.setOnClickListener(v -> {
            Snackbar.make(view, "Support ticket submitted successfully!", Snackbar.LENGTH_LONG).show();
            v.postDelayed(() -> {
                if (isAdded()) Navigation.findNavController(v).navigateUp();
            }, 1500);
        });

        // Placeholder for attachment logic
        view.findViewById(R.id.attachButton).setOnClickListener(v -> {
            Snackbar.make(view, "Attachment feature coming soon!", Snackbar.LENGTH_SHORT).show();
        });
    }
}
