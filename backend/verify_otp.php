<?php
// verify_otp.php - Verify OTP code for email and action
require_once 'db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    send_error("Invalid request method. Only POST is allowed.", "METHOD_NOT_ALLOWED", 405);
}

$email    = isset($_POST['email'])    ? trim($_POST['email'])    : '';
$otp_code = isset($_POST['otp_code']) ? trim($_POST['otp_code']) : '';
$action   = isset($_POST['action'])   ? trim($_POST['action'])   : '';

if (empty($email) || empty($otp_code) || empty($action)) {
    send_error("Email, otp_code, and action parameters are required.", "VALIDATION_ERROR", 422);
}

// ── Step 0: If already VERIFIED and still valid, return success immediately ───
// (Handles double-tap / auto-verify firing a second time)
$stmt_check = $conn->prepare(
    "SELECT id FROM otp_verifications "
  . "WHERE email = ? AND action = ? AND otp_code = 'VERIFIED' AND expires_at > NOW()"
);
$stmt_check->bind_param("ss", $email, $action);
$stmt_check->execute();
$stmt_check->store_result();
$already_verified = ($stmt_check->num_rows > 0);
$stmt_check->close();

if ($already_verified) {
    $conn->close();
    send_success([], "OTP verified successfully.", 200);
}

// ── Step 1: Check if a matching, non-expired OTP exists ──────────────────────
$stmt = $conn->prepare(
    "SELECT id FROM otp_verifications "
  . "WHERE email = ? AND otp_code = ? AND action = ? AND expires_at > NOW()"
);
$stmt->bind_param("sss", $email, $otp_code, $action);
$stmt->execute();
$stmt->store_result();

if ($stmt->num_rows === 0) {
    $stmt->close();
    $conn->close();
    send_error("Invalid or expired OTP code. Please request a new one.", "INVALID_OTP", 400);
}
$stmt->close();

// ── Step 2: Mark as VERIFIED with a generous 30-minute window ────────────────
// This gives the app plenty of time to call signup.php / reset_password.php
// after the dialog closes, regardless of network latency.
$stmt_update = $conn->prepare(
    "UPDATE otp_verifications "
  . "SET otp_code = 'VERIFIED', expires_at = DATE_ADD(NOW(), INTERVAL 30 MINUTE) "
  . "WHERE email = ? AND action = ?"
);
$stmt_update->bind_param("ss", $email, $action);
$stmt_update->execute();

if ($stmt_update->affected_rows === 0) {
    $stmt_update->close();
    $conn->close();
    send_error("Verification state could not be saved. Please try again.", "INTERNAL_SERVER_ERROR", 500);
}
$stmt_update->close();
$conn->close();
send_success([], "OTP verified successfully.", 200);
?>
