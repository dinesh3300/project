<?php
// delete_account.php - Delete doctor account and all associated data
require_once 'db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    send_error("Invalid request method. Only POST is allowed.", "METHOD_NOT_ALLOWED", 405);
}

$email = isset($_POST['email']) ? trim($_POST['email']) : '';

if (empty($email)) {
    send_error("Email parameter is required to delete account.", "VALIDATION_ERROR", 422);
}

// 1. Get profile image path to delete it from disk
$stmt = $conn->prepare("SELECT profile_image FROM doctors WHERE email = ?");
$stmt->bind_param("s", $email);
$stmt->execute();
$stmt->store_result();

if ($stmt->num_rows === 0) {
    $stmt->close();
    send_error("No account found with this email address.", "NOT_FOUND", 404);
}

$stmt->bind_result($profile_image);
$stmt->fetch();
$stmt->close();

// 2. Delete profile image file if exists
if (!empty($profile_image) && file_exists($profile_image) && is_file($profile_image)) {
    @unlink($profile_image);
}

// 3. Get all scan image paths to delete them from disk
$stmt_scans = $conn->prepare("SELECT image_path FROM scans WHERE doctor_email = ?");
$stmt_scans->bind_param("s", $email);
$stmt_scans->execute();
$result_scans = $stmt_scans->get_result();

while ($row = $result_scans->fetch_assoc()) {
    $scan_image = $row['image_path'];
    if (!empty($scan_image) && file_exists($scan_image) && is_file($scan_image)) {
        @unlink($scan_image);
    }
}
$stmt_scans->close();

// Wrap database deletions in a transaction to ensure integrity
$conn->begin_transaction();

try {
    // 4. Delete scans from database
    $stmt_del_scans = $conn->prepare("DELETE FROM scans WHERE doctor_email = ?");
    $stmt_del_scans->bind_param("s", $email);
    $stmt_del_scans->execute();
    $stmt_del_scans->close();

    // 5. Delete OTP verifications from database
    $stmt_del_otp = $conn->prepare("DELETE FROM otp_verifications WHERE email = ?");
    $stmt_del_otp->bind_param("s", $email);
    $stmt_del_otp->execute();
    $stmt_del_otp->close();

    // 6. Delete doctor record from database
    $stmt_del_doc = $conn->prepare("DELETE FROM doctors WHERE email = ?");
    $stmt_del_doc->bind_param("s", $email);
    $stmt_del_doc->execute();
    $stmt_del_doc->close();

    $conn->commit();
    send_success([], "Your account and all associated scans have been permanently deleted.", 200);

} catch (Exception $e) {
    $conn->rollback();
    send_error("Failed to delete doctor account: " . $e->getMessage(), "INTERNAL_SERVER_ERROR", 500);
}

$conn->close();
?>
