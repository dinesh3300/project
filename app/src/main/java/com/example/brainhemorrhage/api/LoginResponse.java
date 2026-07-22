package com.example.brainhemorrhage.api;

public class LoginResponse extends BaseResponse {
    private User user;

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public static class User {
        private String name;
        private String email;
        private String mobile;
        private String gender;
        private String specialty;
        private String profile_image;
        private String hospital;
        private String license_no;
        private int experience;
        private String dob;
        private String address;
        private String theme;
        private String language;

        public String getName() { return name; }
        public String getEmail() { return email; }
        public String getMobile() { return mobile; }
        public String getGender() { return gender; }
        public String getSpecialty() { return specialty; }
        public String getProfile_image() { return profile_image; }
        public String getHospital() { return hospital; }
        public String getLicense_no() { return license_no; }
        public int getExperience() { return experience; }
        public String getDob() { return dob; }
        public String getAddress() { return address; }
        public String getTheme() { return theme; }
        public String getLanguage() { return language; }
    }
}
