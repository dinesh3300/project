<?php
// login.php - Doctor login authentication
require_once 'db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    send_error("Invalid request method. Only POST is allowed.", "METHOD_NOT_ALLOWED", 405);
}

$email = isset($_POST['email']) ? trim($_POST['email']) : '';
$password = isset($_POST['password']) ? trim($_POST['password']) : '';

if (empty($email) || empty($password)) {
    send_error("Email and password are required.", "VALIDATION_ERROR", 422);
}

// Query doctor by email
$stmt = $conn->prepare("SELECT name, email, mobile, gender, password, specialty, profile_image FROM doctors WHERE email = ?");
$stmt->bind_param("s", $email);
$stmt->execute();
$stmt->store_result();

if ($stmt->num_rows > 0) {
    $stmt->bind_result($name, $db_email, $mobile, $gender, $db_password, $specialty, $profile_image);
    $stmt->fetch();
    
    // Verify password hash
    if (password_verify($password, $db_password)) {
        $user_data = [
            "name" => $name,
            "email" => $db_email,
            "mobile" => $mobile,
            "gender" => $gender,
            "specialty" => $specialty ?? "",
            "profile_image" => $profile_image ?? ""
        ];
        
        http_response_code(200);
        echo json_encode([
            "success" => true,
            "status" => "success",
            "message" => "Login successful",
            "data" => $user_data,
            "user" => $user_data,
            "name" => $name,
            "specialty" => $specialty ?? "",
            "profile_image" => $profile_image ?? ""
        ]);
        exit;
    } else {
        send_error("Invalid email or password", "UNAUTHORIZED", 401);
    }
} else {
    send_error("Invalid email or password", "UNAUTHORIZED", 401);
}

$stmt->close();
$conn->close();
?>
