package com.example.brainhemorrhage;

public class ScanTimelineItem {
    private String id;
    private String date;
    private String time;
    private String result;
    private String riskLevel;
    private boolean isLastItem;
    private String imageUri; // URI of the scanned brain image

    public ScanTimelineItem(String id, String date, String time, String result, String riskLevel, boolean isLastItem) {
        this.id = id;
        this.date = date;
        this.time = time;
        this.result = result;
        this.riskLevel = riskLevel;
        this.isLastItem = isLastItem;
        this.imageUri = null;
    }

    public String getId() { return id; }
    public String getDate() { return date; }
    public String getTime() { return time; }
    public String getResult() { return result; }
    public String getRiskLevel() { return riskLevel; }
    public boolean isLastItem() { return isLastItem; }
    public String getImageUri() { return imageUri; }

    public void setLastItem(boolean lastItem) {
        isLastItem = lastItem;
    }
    public void setImageUri(String imageUri) {
        this.imageUri = imageUri;
    }
}
