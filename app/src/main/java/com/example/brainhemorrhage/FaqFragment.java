package com.example.brainhemorrhage;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.List;

public class FaqFragment extends Fragment {

    private FaqAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_faq, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        RecyclerView recyclerView = view.findViewById(R.id.faqRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        List<FaqItem> faqList = new ArrayList<>();
        faqList.add(new FaqItem("How accurate is the AI analysis?", "Our AI models are trained on thousands of clinical cases and achieve high accuracy in detecting various types of brain hemorrhage. However, it should always be reviewed by a radiologist."));
        faqList.add(new FaqItem("What types of scans are supported?", "Currently, Nuerocheck supports CT (Computed Tomography) scans of the brain in standard DICOM or image formats (JPEG/PNG)."));
        faqList.add(new FaqItem("How do I share a diagnostic report?", "After the analysis is complete, click the 'Share' button on the Result screen to export the report as a PDF or image."));
        faqList.add(new FaqItem("Is my data secure?", "Yes, all data is encrypted at rest and during transit. We comply with medical privacy standards to ensure patient confidentiality."));
        faqList.add(new FaqItem("How can I contact support?", "You can go to the Support Center in the Settings menu to submit a ticket or report a technical issue."));

        adapter = new FaqAdapter(faqList);
        recyclerView.setAdapter(adapter);

        TextInputEditText searchInput = view.findViewById(R.id.searchInput);
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }
}
