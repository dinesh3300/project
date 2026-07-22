package com.example.brainhemorrhage;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.net.Uri;
import com.bumptech.glide.Glide;
import com.example.brainhemorrhage.api.RetrofitClient;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ScanTimelineAdapter extends RecyclerView.Adapter<ScanTimelineAdapter.ViewHolder> {

    private List<ScanTimelineItem> scans;
    private OnTimelineItemClickListener listener;

    public interface OnTimelineItemClickListener {
        void onItemClick(ScanTimelineItem scan);
    }

    public ScanTimelineAdapter(List<ScanTimelineItem> scans, OnTimelineItemClickListener listener) {
        this.scans = scans;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_scan_timeline, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ScanTimelineItem scan = scans.get(position);
        
        holder.scanDateTimeText.setText(scan.getDate() + " | " + scan.getTime());
        holder.resultBadge.setText(scan.getResult());
        holder.riskBadge.setText(scan.getRiskLevel() + " RISK");

        // Set colors based on result
        if (scan.getResult().equals("Normal")) {
            holder.timelineIconBackground.setBackgroundTintList(
                holder.itemView.getContext().getResources().getColorStateList(R.color.success_green_10));
            holder.timelineIcon.setImageResource(R.drawable.ic_check_circle);
            holder.timelineIcon.setImageTintList(
                holder.itemView.getContext().getResources().getColorStateList(R.color.success_green));
            holder.resultBadge.setBackgroundResource(R.drawable.badge_success);
            holder.resultBadge.setTextColor(
                holder.itemView.getContext().getResources().getColor(R.color.success_green));
            holder.riskBadge.setBackgroundResource(R.drawable.badge_success);
            holder.riskBadge.setTextColor(
                holder.itemView.getContext().getResources().getColor(R.color.success_green));
        } else {
            holder.timelineIconBackground.setBackgroundTintList(
                holder.itemView.getContext().getResources().getColorStateList(R.color.danger_red_10));
            holder.timelineIcon.setImageResource(R.drawable.ic_warning);
            holder.timelineIcon.setImageTintList(
                holder.itemView.getContext().getResources().getColorStateList(R.color.danger_red));
            holder.resultBadge.setBackgroundResource(R.drawable.badge_danger);
            holder.resultBadge.setTextColor(
                holder.itemView.getContext().getResources().getColor(R.color.danger_red));
            holder.riskBadge.setBackgroundResource(R.drawable.badge_danger);
            holder.riskBadge.setTextColor(
                holder.itemView.getContext().getResources().getColor(R.color.danger_red));
        }

        // Hide timeline line for last item
        if (scan.isLastItem()) {
            holder.timelineLine.setVisibility(View.GONE);
        } else {
            holder.timelineLine.setVisibility(View.VISIBLE);
        }

        // Set thumbnail image
        if (scan.getImageUri() != null) {
            String imagePath = scan.getImageUri();
            // If it's a relative path from the server (e.g. "uploads/...")
            if (!imagePath.startsWith("http") && !imagePath.startsWith("file://") && !imagePath.startsWith("content://")) {
                imagePath = RetrofitClient.BASE_URL + imagePath;
            }

            Glide.with(holder.itemView.getContext())
                .load(imagePath)
                .centerCrop()
                .into(holder.scanThumbnail);
                
            holder.scanThumbnail.setPadding(0, 0, 0, 0); // Remove padding if we have an image
            holder.scanThumbnail.setImageTintList(null); // Remove tint
        } else {
            holder.scanThumbnail.setImageResource(R.drawable.ic_upload);
            holder.scanThumbnail.setPadding(8, 8, 8, 8);
            holder.scanThumbnail.setImageTintList(
                holder.itemView.getContext().getResources().getColorStateList(R.color.text_secondary));
        }

        holder.viewDetailsButton.setOnClickListener(v -> listener.onItemClick(scan));
    }

    @Override
    public int getItemCount() {
        return scans.size();
    }

    /** Replaces the dataset and notifies the adapter — avoids full adapter recreation. */
    public void updateData(List<ScanTimelineItem> newScans) {
        this.scans = newScans;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        View timelineIconBackground, timelineLine;
        ImageView timelineIcon, scanThumbnail;
        TextView scanDateTimeText, resultBadge, riskBadge;
        Button viewDetailsButton;

        ViewHolder(View view) {
            super(view);
            timelineIconBackground = view.findViewById(R.id.timelineIconBackground);
            timelineLine = view.findViewById(R.id.timelineLine);
            timelineIcon = view.findViewById(R.id.timelineIcon);
            scanThumbnail = view.findViewById(R.id.scanThumbnail);
            scanDateTimeText = view.findViewById(R.id.scanDateTimeText);
            resultBadge = view.findViewById(R.id.resultBadge);
            riskBadge = view.findViewById(R.id.riskBadge);
            viewDetailsButton = view.findViewById(R.id.viewDetailsButton);
        }
    }
}
