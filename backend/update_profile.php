<?php
// update_profile.php - Update doctor's name, specialty, and profile picture
require_once 'db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    send_error("Invalid request method. Only POST is allowed.", "METHOD_NOT_ALLOWED", 405);
}

$email = isset($_POST['email']) ? trim($_POST['email']) : '';
$name = isset($_POST['name']) ? trim($_POST['name']) : '';
$specialty = isset($_POST['specialty']) ? trim($_POST['specialty']) : '';

if (empty($email)) {
    send_error("Email is required to update profile.", "VALIDATION_ERROR", 422);
}

// Check if doctor exists
$stmt = $conn->prepare("SELECT id, profile_image FROM doctors WHERE email = ?");
$stmt->bind_param("s", $email);
$stmt->execute();
$stmt->store_result();

if ($stmt->num_rows === 0) {
    $stmt->close();
    send_error("Account not found.", "NOT_FOUND", 404);
}

$stmt->bind_result($doctor_id, $current_profile_image);
$stmt->fetch();
$stmt->close();

$profile_image_path = $current_profile_image;

// Process file upload if profileImage or profile_image is provided
$file_key = isset($_FILES['profileImage']) ? 'profileImage' : (isset($_FILES['profile_image']) ? 'profile_image' : '');
if (!empty($file_key) && $_FILES[$file_key]['error'] === UPLOAD_ERR_OK) {
    $file_tmp = $_FILES[$file_key]['tmp_name'];
    $file_name = $_FILES[$file_key]['name'];
    $file_ext = strtolower(pathinfo($file_name, PATHINFO_EXTENSION));
    
    // Validate file extension
    $allowed_extensions = ['jpg', 'jpeg', 'png', 'webp'];
    if (!in_array($file_ext, $allowed_extensions)) {
        send_error("Invalid image format. Allowed formats: " . implode(', ', $allowed_extensions), "INVALID_IMAGE_FORMAT", 400);
    }
    
    // Create uploads/profiles directory if it doesn't exist
    $upload_dir = 'uploads/profiles/';
    if (!is_dir($upload_dir)) {
        mkdir($upload_dir, 0777, true);
    }
    
    // Generate clean unique filename
    $email_prefix = preg_replace('/[^a-zA-Z0-9]/', '_', explode('@', $email)[0]);
    $new_file_name = 'profile_' . $email_prefix . '_' . time() . '.' . $file_ext;
    $dest_path = $upload_dir . $new_file_name;
    
    if (move_uploaded_file($file_tmp, $dest_path)) {
        // Delete the old profile picture if it exists and is different
        if (!empty($current_profile_image) && file_exists($current_profile_image) && is_file($current_profile_image)) {
            @unlink($current_profile_image);
        }
        $profile_image_path = $dest_path;
    } else {
        send_error("Failed to save profile image.", "FILE_SAVE_ERROR", 500);
    }
}

// Update name, specialty, and profile image
$stmt_update = $conn->prepare("UPDATE doctors SET name = ?, specialty = ?, profile_image = ? WHERE email = ?");
$stmt_update->bind_param("ssss", $name, $specialty, $profile_image_path, $email);

if ($stmt_update->execute()) {
    http_response_code(200);
    echo json_encode([
        "success" => true,
        "status" => "success",
        "message" => "Profile updated successfully.",
        "data" => [
            "profile_image" => $profile_image_path
        ],
        "profile_image" => $profile_image_path
    ]);
    exit;
} else {
    send_error("Failed to update profile details in database.", "INTERNAL_SERVER_ERROR", 500);
}

$stmt_update->close();
$conn->close();
?>
