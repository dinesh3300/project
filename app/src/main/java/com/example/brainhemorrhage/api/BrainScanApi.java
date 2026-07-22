package com.example.brainhemorrhage.api;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Query;

public interface BrainScanApi {

    @FormUrlEncoded
    @POST("login.php")
    Call<LoginResponse> login(
            @Field("email") String email,
            @Field("password") String password
    );

    @FormUrlEncoded
    @POST("signup.php")
    Call<BaseResponse> signup(
            @Field("name") String name,
            @Field("email") String email,
            @Field("mobile") String mobile,
            @Field("gender") String gender,
            @Field("password") String password,
            @Field("otp_code") String otpCode   // sent for atomic server-side OTP check
    );

    @FormUrlEncoded
    @POST("delete_account.php")
    Call<BaseResponse> deleteAccount(
            @Field("email") String email
    );

    @GET("get_scans.php")
    Call<ScanResponse> getPatientScans(
            @Query("doctor_email") String doctorEmail,
            @Query("patient_id") String patientId,
            @Query("patient_name") String patientName,
            @Query("patient_age") String patientAge,
            @Query("patient_gender") String patientGender
    );

    @Multipart
    @POST("upload_scan.php")
    Call<BaseResponse> uploadScan(
            @Part("doctor_email") RequestBody doctorEmail,
            @Part("patient_id") RequestBody patientId,
            @Part("patient_name") RequestBody patientName,
            @Part("patient_age") RequestBody patientAge,
            @Part("patient_gender") RequestBody patientGender,
            @Part("result") RequestBody result,
            @Part("risk_level") RequestBody riskLevel,
            @Part("notes") RequestBody notes,
            @Part("doctor_review") RequestBody doctorReview,
            @Part("intraventricular") RequestBody intraventricular,
            @Part("intraparenchymal") RequestBody intraparenchymal,
            @Part("subarachnoid") RequestBody subarachnoid,
            @Part("epidural") RequestBody epidural,
            @Part("subdural") RequestBody subdural,
            @Part MultipartBody.Part image
    );

    @Multipart
    @POST("update_profile.php")
    Call<BaseResponse> updateProfile(
            @Part("email") RequestBody email,
            @Part("name") RequestBody name,
            @Part("mobile") RequestBody mobile,
            @Part("gender") RequestBody gender,
            @Part("specialty") RequestBody specialty,
            @Part("hospital") RequestBody hospital,
            @Part("license_no") RequestBody licenseNo,
            @Part("experience") RequestBody experience,
            @Part("dob") RequestBody dob,
            @Part("address") RequestBody address,
            @Part MultipartBody.Part profileImage
    );

    /** Text-only profile update (no photo — avoids passing null MultipartBody.Part) */
    @FormUrlEncoded
    @POST("update_profile.php")
    Call<BaseResponse> updateProfileTextOnly(
            @Field("email") String email,
            @Field("name") String name,
            @Field("mobile") String mobile,
            @Field("gender") String gender,
            @Field("specialty") String specialty,
            @Field("hospital") String hospital,
            @Field("license_no") String licenseNo,
            @Field("experience") int experience,
            @Field("dob") String dob,
            @Field("address") String address
    );


    @FormUrlEncoded
    @POST("send_otp.php")
    Call<BaseResponse> sendOtp(
            @Field("email") String email,
            @Field("action") String action
    );

    @FormUrlEncoded
    @POST("verify_otp.php")
    Call<BaseResponse> verifyOtp(
            @Field("email") String email,
            @Field("otp_code") String otpCode,
            @Field("action") String action
    );

    @FormUrlEncoded
    @POST("reset_password.php")
    Call<BaseResponse> resetPassword(
            @Field("email") String email,
            @Field("otp_code") String otpCode,
            @Field("new_password") String newPassword
    );

    @GET("get_profile.php")
    Call<LoginResponse> getProfile(
            @Query("email") String email
    );

    @FormUrlEncoded
    @POST("update_settings.php")
    Call<BaseResponse> updateSettings(
            @Field("email") String email,
            @Field("theme") String theme,
            @Field("language") String language,
            @Field("daily_summary") String dailySummary,
            @Field("notif_sound") String notifSound,
            @Field("notif_vibration") String notifVibration
    );

    @GET("get_notifications.php")
    Call<BaseResponse> getNotifications(
            @Query("email") String email
    );

    /**
     * Validates that the locally-cached email still exists in the server database.
     * Called at every app launch (SplashFragment) to detect stale sessions caused
     * by DB resets or account deletions. Returns "success" + fresh user data if
     * the account exists, or "error" if it no longer exists.
     */
    @FormUrlEncoded
    @POST("check_user.php")
    Call<LoginResponse> checkUser(
            @Field("email") String email
    );
}
