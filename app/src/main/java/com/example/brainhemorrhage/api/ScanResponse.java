package com.example.brainhemorrhage.api;

import java.util.List;

public class ScanResponse {
    private String status;
    private List<ScanItemDto> data;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<ScanItemDto> getData() { return data; }
    public void setData(List<ScanItemDto> data) { this.data = data; }

    public static class ScanItemDto {
        private String id;
        private String patient_id;
        private String doctor_email;
        private String patient_name;
        private String patient_age;
        private String patient_gender;
        private String date_added;
        private String time_added;
        private String result;
        private String risk_level;
        private String image_path;
        private String notes;
        private String doctor_review;
        private double intraventricular;
        private double intraparenchymal;
        private double subarachnoid;
        private double epidural;
        private double subdural;

        public String getId() { return id; }
        public String getPatient_id() { return patient_id; }
        public double getIntraventricular() { return intraventricular; }
        public double getIntraparenchymal() { return intraparenchymal; }
        public double getSubarachnoid() { return subarachnoid; }
        public double getEpidural() { return epidural; }
        public double getSubdural() { return subdural; }
        public String getDoctor_email() { return doctor_email; }
        public String getPatient_name() { return patient_name; }
        public String getPatient_age() { return patient_age; }
        public String getPatient_gender() { return patient_gender; }
        public String getDate_added() { return date_added; }
        public String getTime_added() { return time_added; }
        public String getResult() { return result; }
        public String getRisk_level() { return risk_level; }
        public String getImage_path() { return image_path; }
        public String getNotes() { return notes; }
        public String getDoctor_review() { return doctor_review; }
    }
}
