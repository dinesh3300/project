<?php
// check_user.php - Lightweight session validation endpoint.
// The Android app calls this at startup to verify that the locally-cached
// email address still exists in the database. If the DB was wiped / the
// account was deleted the app clears its local session and shows Login.
require_once 'db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    send_error("Invalid request method. Only POST is allowed.", "METHOD_NOT_ALLOWED", 405);
}

$email = isset($_POST['email']) ? trim($_POST['email']) : '';

if (empty($email)) {
    send_error("Email parameter is required.", "VALIDATION_ERROR", 422);
}

// Check whether this email exists in the doctors table
$stmt = $conn->prepare(
    "SELECT id, name, email, mobile, gender, specialty, profile_image "
  . "FROM doctors WHERE email = ? LIMIT 1"
);
$stmt->bind_param("s", $email);
$stmt->execute();
$result = $stmt->get_result();

if ($result->num_rows === 0) {
    // Account no longer exists — tell the app to clear its local session
    $stmt->close();
    $conn->close();
    send_error("Account not found. Please log in again.", "NOT_FOUND", 404);
}

$row = $result->fetch_assoc();
$stmt->close();
$conn->close();

// Return fresh user data so the app can refresh its cached profile
$user_data = [
    "name"          => $row['name'],
    "email"         => $row['email'],
    "mobile"        => $row['mobile'],
    "gender"        => $row['gender'],
    "specialty"     => $row['specialty']      ?? '',
    "profile_image" => $row['profile_image']  ?? ''
];

http_response_code(200);
echo json_encode([
    "success" => true,
    "status"  => "success",
    "message" => "User verified successfully",
    "data"    => $user_data,
    "user"    => $user_data
]);
exit;
?>
