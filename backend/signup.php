<?php
// signup.php - Register a new doctor; OTP is verified atomically here
require_once 'db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    send_error("Invalid request method. Only POST is allowed.", "METHOD_NOT_ALLOWED", 405);
}

$name     = isset($_POST['name'])     ? trim($_POST['name'])     : '';
$email    = isset($_POST['email'])    ? trim($_POST['email'])    : '';
$mobile   = isset($_POST['mobile'])   ? trim($_POST['mobile'])   : '';
$gender   = isset($_POST['gender'])   ? trim($_POST['gender'])   : '';
$password = isset($_POST['password']) ? trim($_POST['password']) : '';
$otp_code = isset($_POST['otp_code']) ? trim($_POST['otp_code']) : '';

if (empty($name) || empty($email) || empty($mobile) || empty($gender) || empty($password)) {
    send_error("All registration fields (name, email, mobile, gender, password) are required.", "VALIDATION_ERROR", 422);
}

// ── DIAGNOSTIC: Dump every OTP row for this email + current DB time ──────────
$debug_rows = [];
$debug_stmt = $conn->prepare("SELECT id, email, otp_code, action, expires_at FROM otp_verifications WHERE email = ?");
$debug_stmt->bind_param("s", $email);
$debug_stmt->execute();
$debug_result = $debug_stmt->get_result();
while ($r = $debug_result->fetch_assoc()) {
    $debug_rows[] = $r;
}
$debug_stmt->close();

$time_result = $conn->query("SELECT NOW() as db_now");
$db_now = $time_result->fetch_assoc()['db_now'];
// ─────────────────────────────────────────────────────────────────────────────

// ── 1. Check if email already exists ─────────────────────────────────────────
$stmt = $conn->prepare("SELECT id FROM doctors WHERE email = ?");
$stmt->bind_param("s", $email);
$stmt->execute();
$stmt->store_result();

if ($stmt->num_rows > 0) {
    $stmt->close();
    send_error("An account with this email address already exists.", "EMAIL_ALREADY_EXISTS", 409);
}
$stmt->close();

// ── 2. OTP check — Pass A: raw code ──────────────────────────────────────────
$otp_ok = false;

if (!empty($otp_code) && $otp_code !== 'VERIFIED') {
    $stmt = $conn->prepare(
        "SELECT id FROM otp_verifications "
      . "WHERE email = ? AND otp_code = ? AND action = 'signup' AND expires_at > NOW()"
    );
    $stmt->bind_param("ss", $email, $otp_code);
    $stmt->execute();
    $stmt->store_result();
    $otp_ok = ($stmt->num_rows > 0);
    $stmt->close();
}

// ── 3. OTP check — Pass B: VERIFIED state ────────────────────────────────────
if (!$otp_ok) {
    $stmt = $conn->prepare(
        "SELECT id FROM otp_verifications "
      . "WHERE email = ? AND action = 'signup' AND otp_code = 'VERIFIED' AND expires_at > NOW()"
    );
    $stmt->bind_param("s", $email);
    $stmt->execute();
    $stmt->store_result();
    $otp_ok = ($stmt->num_rows > 0);
    $stmt->close();
}

// ── 4. Fail with FULL DEBUG info so we can see exactly what went wrong ────────
if (!$otp_ok) {
    send_error("Email verification failed. Please go back and re-enter your OTP.", "INVALID_OTP", 400);
}

// ── 5. Hash the password and save ────────────────────────────────────────────
$hashed_password = password_hash($password, PASSWORD_DEFAULT);
$stmt = $conn->prepare("INSERT INTO doctors (name, email, mobile, gender, password) VALUES (?, ?, ?, ?, ?)");
$stmt->bind_param("sssss", $name, $email, $mobile, $gender, $hashed_password);

if ($stmt->execute()) {
    $stmt_clean = $conn->prepare("DELETE FROM otp_verifications WHERE email = ? AND action = 'signup'");
    $stmt_clean->bind_param("s", $email);
    $stmt_clean->execute();
    $stmt_clean->close();

    send_success([], "Account created successfully. You can now login.", 201);
} else {
    send_error("Registration failed. Please try again.", "INTERNAL_SERVER_ERROR", 500);
}

$stmt->close();
$conn->close();
?>
