package com.example.brainhemorrhage;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class StatsAdapter extends RecyclerView.Adapter<StatsAdapter.ViewHolder> {

    private List<StatItem> stats;

    public StatsAdapter(List<StatItem> stats) {
        this.stats = stats;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_stat_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StatItem stat = stats.get(position);
        holder.labelText.setText(stat.getLabel());
        holder.valueText.setText(stat.getValue());
        holder.iconImage.setImageResource(stat.getIconRes());
        
        if (stat.hasTrend()) {
            holder.trendText.setVisibility(View.VISIBLE);
            holder.trendText.setText(stat.getSubtext());
            holder.subtextText.setVisibility(View.GONE);
        } else {
            holder.trendText.setVisibility(View.GONE);
            holder.subtextText.setVisibility(View.VISIBLE);
            holder.subtextText.setText(stat.getSubtext());
        }
    }

    @Override
    public int getItemCount() {
        return stats.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView iconImage;
        TextView labelText, valueText, trendText, subtextText;

        ViewHolder(View view) {
            super(view);
            iconImage = view.findViewById(R.id.iconImage);
            labelText = view.findViewById(R.id.labelText);
            valueText = view.findViewById(R.id.valueText);
            trendText = view.findViewById(R.id.trendText);
            subtextText = view.findViewById(R.id.subtextText);
        }
    }
}
