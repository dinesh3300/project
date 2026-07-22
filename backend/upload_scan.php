<?php
// upload_scan.php - Upload scan results and image
require_once 'db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    send_error("Invalid request method. Only POST is allowed.", "METHOD_NOT_ALLOWED", 405);
}

$doctor_email = isset($_POST['doctor_email']) ? trim($_POST['doctor_email']) : '';
$patient_id = isset($_POST['patient_id']) ? trim($_POST['patient_id']) : '';
$patient_name = isset($_POST['patient_name']) ? trim($_POST['patient_name']) : '';
$patient_age = isset($_POST['patient_age']) ? trim($_POST['patient_age']) : '';
$patient_gender = isset($_POST['patient_gender']) ? trim($_POST['patient_gender']) : '';
$result = isset($_POST['result']) ? trim($_POST['result']) : '';
$risk_level = isset($_POST['risk_level']) ? trim($_POST['risk_level']) : '';

$intraventricular = isset($_POST['intraventricular']) ? (double)$_POST['intraventricular'] : 0.0;
$intraparenchymal = isset($_POST['intraparenchymal']) ? (double)$_POST['intraparenchymal'] : 0.0;
$subarachnoid = isset($_POST['subarachnoid']) ? (double)$_POST['subarachnoid'] : 0.0;
$epidural = isset($_POST['epidural']) ? (double)$_POST['epidural'] : 0.0;
$subdural = isset($_POST['subdural']) ? (double)$_POST['subdural'] : 0.0;

if (empty($doctor_email) || empty($patient_id) || empty($patient_name) || empty($result) || empty($risk_level)) {
    send_error("Required fields (doctor_email, patient_id, patient_name, result, risk_level) are missing.", "VALIDATION_ERROR", 422);
}

$image_path = '';

// Process the scan image file upload
if (isset($_FILES['image']) && $_FILES['image']['error'] === UPLOAD_ERR_OK) {
    $file_tmp = $_FILES['image']['tmp_name'];
    $file_name = $_FILES['image']['name'];
    $file_ext = strtolower(pathinfo($file_name, PATHINFO_EXTENSION));
    
    // Validate image extension
    $allowed_extensions = ['jpg', 'jpeg', 'png', 'webp'];
    if (!in_array($file_ext, $allowed_extensions)) {
        send_error("Invalid image format. Allowed formats: " . implode(', ', $allowed_extensions), "INVALID_IMAGE_FORMAT", 400);
    }
    
    // Create uploads/scans directory if it doesn't exist
    $upload_dir = 'uploads/scans/';
    if (!is_dir($upload_dir)) {
        mkdir($upload_dir, 0777, true);
    }
    
    // Generate unique name for the scan
    $new_file_name = 'scan_' . $patient_id . '_' . time() . '.' . $file_ext;
    $dest_path = $upload_dir . $new_file_name;
    
    if (move_uploaded_file($file_tmp, $dest_path)) {
        $image_path = $dest_path;
    } else {
        send_error("Failed to save uploaded scan image.", "FILE_SAVE_ERROR", 500);
    }
} else {
    send_error("Scan image file is required and was not uploaded successfully.", "VALIDATION_ERROR", 422);
}

// Generate date and time added
$date_added = date('d M Y');
$time_added = date('h:i A');

// Insert scan record into database
$stmt = $conn->prepare("INSERT INTO scans (doctor_email, patient_id, patient_name, patient_age, patient_gender, result, risk_level, image_path, date_added, time_added, intraventricular, intraparenchymal, subarachnoid, epidural, subdural) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
$stmt->bind_param("ssssssssssddddd", $doctor_email, $patient_id, $patient_name, $patient_age, $patient_gender, $result, $risk_level, $image_path, $date_added, $time_added, $intraventricular, $intraparenchymal, $subarachnoid, $epidural, $subdural);

if ($stmt->execute()) {
    $inserted_id = $stmt->insert_id;
    send_success(["patient_id" => $patient_id, "id" => (string)$inserted_id], "Scan uploaded and saved successfully.", 201);
} else {
    send_error("Failed to save scan record to database.", "INTERNAL_SERVER_ERROR", 500);
}

$stmt->close();
$conn->close();
?>
