package com.example.brainhemorrhage;

public class StatItem {
    private String label;
    private String value;
    private String subtext;
    private boolean hasTrend;
    private int iconRes;

    public StatItem(String label, String value, String subtext, boolean hasTrend, int iconRes) {
        this.label = label;
        this.value = value;
        this.subtext = subtext;
        this.hasTrend = hasTrend;
        this.iconRes = iconRes;
    }

    public String getLabel() { return label; }
    public String getValue() { return value; }
    public String getSubtext() { return subtext; }
    public boolean hasTrend() { return hasTrend; }
    public int getIconRes() { return iconRes; }
}
