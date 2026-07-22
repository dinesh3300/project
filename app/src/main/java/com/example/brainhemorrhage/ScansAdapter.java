package com.example.brainhemorrhage;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;

import com.example.brainhemorrhage.api.RetrofitClient;
import androidx.annotation.NonNull;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ScansAdapter extends RecyclerView.Adapter<ScansAdapter.ViewHolder> {

    private List<ScanItem> scans;
    private OnScanClickListener listener;

    public interface OnScanClickListener {
        void onScanClick(ScanItem scan);
    }

    public ScansAdapter(List<ScanItem> scans, OnScanClickListener listener) {
        this.scans = scans;
        this.listener = listener;
    }

    public void updateData(List<ScanItem> newScans) {
        this.scans = newScans;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_scan, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ScanItem scan = scans.get(position);
        holder.patientName.setText(scan.getPatientName());
        holder.patientId.setText(holder.itemView.getContext().getString(R.string.label_patient_id_format, scan.getDbPatientId()));
        holder.scanDate.setText(scan.getDate());
        holder.resultBadge.setText(scan.getResult());
        
        if (scan.getResult() != null && scan.getResult().toLowerCase().contains("normal") && !scan.getResult().toLowerCase().contains("abnormal") && !scan.getResult().toLowerCase().contains("hemorrhage")) {
            holder.resultBadge.setBackgroundResource(R.drawable.badge_success);
            holder.resultBadge.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.success));
        } else {
            holder.resultBadge.setBackgroundResource(R.drawable.badge_danger);
            holder.resultBadge.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.error));
        }

        // Load image using Glide
        if (scan.getImagePath() != null && !scan.getImagePath().isEmpty()) {
            String imageUrl = scan.getImagePath();
            // Prepend BASE_URL if it's a relative server path
            if (!imageUrl.startsWith("http") && !imageUrl.startsWith("file") && !imageUrl.startsWith("content")) {
                imageUrl = RetrofitClient.BASE_URL + imageUrl;
            }
            
            Glide.with(holder.itemView.getContext())
                .load(imageUrl)
                .placeholder(R.drawable.logo)
                .error(R.drawable.logo)
                .centerCrop()
                .into(holder.scanThumbnail);
        } else {
            holder.scanThumbnail.setImageResource(R.drawable.logo);
        }



        holder.itemView.setOnClickListener(v -> listener.onScanClick(scan));
    }

    @Override
    public int getItemCount() {
        return scans.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView patientName, patientId, scanDate, resultBadge;
        ImageView scanThumbnail;

        ViewHolder(View view) {
            super(view);
            patientName = view.findViewById(R.id.patientName);
            patientId = view.findViewById(R.id.patientId);
            scanDate = view.findViewById(R.id.scanDate);
            resultBadge = view.findViewById(R.id.resultStatus);
            scanThumbnail = view.findViewById(R.id.scanThumbnail);
        }
    }

}
