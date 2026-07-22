package com.example.brainhemorrhage;

public class ScanItem {
    private String id;
    private String patientId;
    private String patientName;
    private String date;
    private String result;
    private String imagePath;
    private String age;
    private String gender;
    private String dbPatientId;

    public ScanItem(int id, String patientId, String patientName, String result, String date, String imagePath) {
        this.id = String.valueOf(id);
        this.patientId = patientId;
        this.patientName = patientName != null && !patientName.isEmpty() ? patientName : patientId;
        this.result = result;
        this.date = date;
        this.imagePath = imagePath;
    }

    public ScanItem(String id, String patientName, String date, String result) {
        this.id = id;
        this.patientId = patientName;
        this.patientName = patientName;
        this.date = date;
        this.result = result;
        this.imagePath = "";
    }

    public String getId() { return id; }
    public String getPatientId() { return patientId; }
    public String getPatientName() { return patientName; }
    public String getDate() { return date; }
    public String getResult() { return result; }
    public String getImagePath() { return imagePath; }
    
    public void setAge(String age) { this.age = age; }
    public String getAge() { return age; }
    
    public void setGender(String gender) { this.gender = gender; }
    public String getGender() { return gender; }
    
    public void setDbPatientId(String dbPatientId) { this.dbPatientId = dbPatientId; }
    public String getDbPatientId() { return dbPatientId; }
}
