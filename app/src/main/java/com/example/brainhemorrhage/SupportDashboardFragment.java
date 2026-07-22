package com.example.brainhemorrhage;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.google.android.material.appbar.MaterialToolbar;

public class SupportDashboardFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_support_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        view.findViewById(R.id.contactSupportCard).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_supportDashboard_to_contactSupport));

        view.findViewById(R.id.faqCard).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_supportDashboard_to_faq));

        view.findViewById(R.id.helpCenterCard).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_supportDashboard_to_helpCenter));

        view.findViewById(R.id.bugReportCard).setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("category", "Bug Report");
            Navigation.findNavController(v).navigate(R.id.action_supportDashboard_to_contactSupport, args);
        });
    }
}
