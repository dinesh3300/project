<?php
// get_scans.php - Fetch scans with optional filters
require_once 'db.php';

// Read GET query parameters
$doctor_email = isset($_GET['doctor_email']) ? trim($_GET['doctor_email']) : '';
$patient_id = isset($_GET['patient_id']) ? trim($_GET['patient_id']) : '';
$patient_name = isset($_GET['patient_name']) ? trim($_GET['patient_name']) : '';
$patient_age = isset($_GET['patient_age']) ? trim($_GET['patient_age']) : '';
$patient_gender = isset($_GET['patient_gender']) ? trim($_GET['patient_gender']) : '';

// Build query dynamically with valid database schema columns
$query = "SELECT id, doctor_id, doctor_email, patient_id, patient_name, patient_age, patient_gender, result, risk_level, image_path, notes, doctor_review, intraventricular, intraparenchymal, subarachnoid, epidural, subdural, created_at FROM scans WHERE 1=1";
$params = [];
$types = "";

// Retrieve all scans in database so all saved scans are visible in dashboard and history
// If filtering by doctor is explicitly requested via filter_doctor parameter, apply filter
if (!empty($_GET['filter_doctor'])) {
    $query .= " AND (doctor_email = ? OR doctor_email = '' OR doctor_email IS NULL)";
    $params[] = $doctor_email;
    $types .= "s";
}

if (!empty($patient_id)) {
    $query .= " AND patient_id LIKE ?";
    $params[] = "%" . $patient_id . "%";
    $types .= "s";
}

if (!empty($patient_name)) {
    $query .= " AND patient_name LIKE ?";
    $params[] = "%" . $patient_name . "%";
    $types .= "s";
}

if (!empty($patient_age)) {
    $query .= " AND patient_age = ?";
    $params[] = $patient_age;
    $types .= "s";
}

if (!empty($patient_gender)) {
    $query .= " AND patient_gender = ?";
    $params[] = $patient_gender;
    $types .= "s";
}

// Order by most recent scans first
$query .= " ORDER BY id DESC";

if ($conn) {
    $stmt = $conn->prepare($query);
    if ($stmt) {
        if (!empty($types)) {
            $stmt->bind_param($types, ...$params);
        }

        $stmt->execute();
        $result = $stmt->get_result();

        $scans = [];
        while ($row = $result->fetch_assoc()) {
            $row['id'] = (string)$row['id'];
            $createdAt = strtotime($row['created_at']);
            $row['date_added'] = date('d M Y', $createdAt);
            $row['time_added'] = date('h:i A', $createdAt);
            $row['intraventricular'] = (double)$row['intraventricular'];
            $row['intraparenchymal'] = (double)$row['intraparenchymal'];
            $row['subarachnoid'] = (double)$row['subarachnoid'];
            $row['epidural'] = (double)$row['epidural'];
            $row['subdural'] = (double)$row['subdural'];
            $scans[] = $row;
        }

        send_success($scans, "Scans retrieved successfully.");
        $stmt->close();
    } else {
        send_error("Database statement error: " . $conn->error, "SQL_ERROR", 500);
    }
} else {
    send_success([], "No database connection available.");
}
?>
