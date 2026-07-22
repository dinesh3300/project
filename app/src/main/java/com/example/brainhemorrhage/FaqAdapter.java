package com.example.brainhemorrhage;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class FaqAdapter extends RecyclerView.Adapter<FaqAdapter.ViewHolder> {

    private List<FaqItem> fullList;
    private List<FaqItem> displayList;

    public FaqAdapter(List<FaqItem> list) {
        this.fullList = list;
        this.displayList = new ArrayList<>(list);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_faq, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FaqItem item = displayList.get(position);
        holder.questionText.setText(item.getQuestion());
        holder.answerText.setText(item.getAnswer());

        holder.answerLayout.setVisibility(item.isExpanded() ? View.VISIBLE : View.GONE);
        holder.expandIcon.setRotation(item.isExpanded() ? 90f : 0f);

        holder.headerLayout.setOnClickListener(v -> {
            item.setExpanded(!item.isExpanded());
            notifyItemChanged(position);
        });
    }

    @Override
    public int getItemCount() { return displayList.size(); }

    public void filter(String query) {
        displayList.clear();
        if (query.isEmpty()) {
            displayList.addAll(fullList);
        } else {
            String lowerQuery = query.toLowerCase();
            for (FaqItem item : fullList) {
                if (item.getQuestion().toLowerCase().contains(lowerQuery) ||
                    item.getAnswer().toLowerCase().contains(lowerQuery)) {
                    displayList.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView questionText, answerText;
        LinearLayout headerLayout, answerLayout;
        ImageView expandIcon;

        ViewHolder(View view) {
            super(view);
            questionText = view.findViewById(R.id.questionText);
            answerText = view.findViewById(R.id.answerText);
            headerLayout = view.findViewById(R.id.headerLayout);
            answerLayout = view.findViewById(R.id.answerLayout);
            expandIcon = view.findViewById(R.id.expandIcon);
        }
    }
}
