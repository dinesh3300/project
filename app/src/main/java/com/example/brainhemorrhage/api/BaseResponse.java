package com.example.brainhemorrhage.api;

public class BaseResponse {
    private String status;
    private String message;
    private String patient_id;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getPatient_id() { return patient_id; }
    public void setPatient_id(String patient_id) { this.patient_id = patient_id; }

    private String profile_image;
    public String getProfile_image() { return profile_image; }
    public void setProfile_image(String profile_image) { this.profile_image = profile_image; }
}

