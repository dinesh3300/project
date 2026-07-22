package com.example.brainhemorrhage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ScanDataManager {
    private static ScanDataManager instance;

    private ScanDataManager() {
        // Replaced in-memory initialization with persistent SQLite storage
    }

    public static synchronized ScanDataManager getInstance() {
        if (instance == null) {
            instance = new ScanDataManager();
        }
        return instance;
    }

    public List<ScanTimelineItem> getPatientScans(String patientId) {
        return DatabaseHelper.getInstance(BrainHemorrhageApp.getAppContext()).getScansForPatient(patientId);
    }

    public void addScanForPatient(String patientId, String result, String riskLevel, String imageUri) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        Date now = new Date();
        String currentDate = dateFormat.format(now);
        String currentTime = timeFormat.format(now);

        // Fallback insert to support legacy fragments that call this signature
        DatabaseHelper.getInstance(BrainHemorrhageApp.getAppContext()).insertScan(
            patientId,
            "Patient",
            "0",
            "Not Specified",
            currentDate,
            currentTime,
            result,
            riskLevel,
            imageUri,
            0.0f, 0.0f, 0.0f, 0.0f, 0.0f
        );
    }

    public int getTotalScansForPatient(String patientId) {
        return getPatientScans(patientId).size();
    }

    public void clearAllData() {
        DatabaseHelper.getInstance(BrainHemorrhageApp.getAppContext()).clearAllData();
    }
}
