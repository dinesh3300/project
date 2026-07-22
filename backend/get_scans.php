<?php
// get_scans.php - Fetch scans with optional filters
require_once 'db.php';

// Since this is a GET request, we read parameters from $_GET
$doctor_email = isset($_GET['doctor_email']) ? trim($_GET['doctor_email']) : '';
$patient_id = isset($_GET['patient_id']) ? trim($_GET['patient_id']) : '';
$patient_name = isset($_GET['patient_name']) ? trim($_GET['patient_name']) : '';
$patient_age = isset($_GET['patient_age']) ? trim($_GET['patient_age']) : '';
$patient_gender = isset($_GET['patient_gender']) ? trim($_GET['patient_gender']) : '';

// We build the query dynamically
$query = "SELECT id, doctor_email, patient_id, patient_name, patient_age, patient_gender, result, risk_level, image_path, date_added, time_added, intraventricular, intraparenchymal, subarachnoid, epidural, subdural FROM scans WHERE 1=1";
$params = [];
$types = "";

if (!empty($doctor_email)) {
    $query .= " AND doctor_email = ?";
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

$stmt = $conn->prepare($query);

if (!empty($types)) {
    $stmt->bind_param($types, ...$params);
}

$stmt->execute();
$result = $stmt->get_result();

$scans = [];
while ($row = $result->fetch_assoc()) {
    // Cast ID to string to match Retrofit's expectation if necessary
    $row['id'] = (string)$row['id'];
    $row['intraventricular'] = (double)$row['intraventricular'];
    $row['intraparenchymal'] = (double)$row['intraparenchymal'];
    $row['subarachnoid'] = (double)$row['subarachnoid'];
    $row['epidural'] = (double)$row['epidural'];
    $row['subdural'] = (double)$row['subdural'];
    $scans[] = $row;
}

send_success($scans, "Scans retrieved successfully.");

$stmt->close();
$conn->close();
?>
