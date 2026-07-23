<?php
/**
 * analyze.php — Nuerocheck AI Inference Endpoint
 *
 * Accepts: POST multipart/form-data with field 'image' (JPEG/PNG file)
 * Returns: JSON with inference results (same fields as ScanResult on the web frontend)
 *
 * Delegates to inference.py using Python's ai-edge-litert (TFLite) to run
 * the same 3-stage pipeline as the Android app:
 *   Stage 1: brain_ct_classifier.tflite (gatekeeper)
 *   Stage 2: hemorrhage_detector.tflite  (YOLO NMS)
 *   Stage 3: Hemorrhage.tflite           (subtype ODT signature)
 */

$ALLOW_NO_DB = true;
require_once 'db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    send_error("Only POST method is allowed", "METHOD_NOT_ALLOWED", 405);
}

// ── Validate uploaded image ──────────────────────────────────────────────────
if (!isset($_FILES['image']) || $_FILES['image']['error'] !== UPLOAD_ERR_OK) {
    send_error('No image uploaded or upload error: ' . ($_FILES['image']['error'] ?? 'none'), "VALIDATION_ERROR", 422);
}

$allowed = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp'];
$mime    = mime_content_type($_FILES['image']['tmp_name']);
if (!in_array(strtolower($mime), $allowed)) {
    send_error("Unsupported image type: $mime", "INVALID_IMAGE_FORMAT", 400);
}

// ── Save image to a temp location ───────────────────────────────────────────
$ext      = 'jpg';
$tmpDir   = sys_get_temp_dir();
$tmpFile  = $tmpDir . DIRECTORY_SEPARATOR . 'nuerocheck_' . uniqid() . '.' . $ext;

if (!@move_uploaded_file($_FILES['image']['tmp_name'], $tmpFile)) {
    if (!@copy($_FILES['image']['tmp_name'], $tmpFile)) {
        send_error('Failed to save uploaded file to temp directory', "FILE_SAVE_ERROR", 500);
    }
}

// ── Build the Python command ─────────────────────────────────────────────────
$scriptPath = __DIR__ . DIRECTORY_SEPARATOR . 'inference.py';
$scriptPath = escapeshellarg($scriptPath);
$imagePath  = escapeshellarg($tmpFile);

// Dynamically resolve Python executable across virtualenvs, system installation paths, and PATH
$possiblePythons = [
    'C:\\Users\\ADMIN\\AppData\\Local\\Programs\\Python\\Python311\\python.exe',
    'C:\\Users\\ADMIN\\AppData\\Local\\Programs\\Python\\Python313\\python.exe',
    __DIR__ . '/venv/Scripts/python.exe',
    __DIR__ . '/../venv/Scripts/python.exe',
    __DIR__ . '/venv/bin/python',
];

$pythonCmd = '';
foreach ($possiblePythons as $pyPath) {
    if (file_exists($pyPath)) {
        $pythonCmd = escapeshellarg($pyPath);
        break;
    }
}

if (empty($pythonCmd)) {
    // Check system 'python' command
    exec('python --version 2>&1', $pyOut, $pyCode);
    if ($pyCode === 0) {
        $pythonCmd = 'python';
    } else {
        exec('python3 --version 2>&1', $py3Out, $py3Code);
        $pythonCmd = ($py3Code === 0) ? 'python3' : 'python';
    }
}

// Run inference — 2>&1 redirects stderr into output for debugging
$command = "$pythonCmd $scriptPath $imagePath 2>&1";
$output  = [];
$exitCode = 0;
exec($command, $output, $exitCode);

// ── Clean up temp file ───────────────────────────────────────────────────────
@unlink($tmpFile);

// ── Parse result ─────────────────────────────────────────────────────────────
$rawOutput = implode("\n", $output);

// Find the JSON line (last line that starts with '{')
$jsonLine = '';
foreach (array_reverse($output) as $line) {
    $line = trim($line);
    if ($line !== '' && $line[0] === '{') {
        $jsonLine = $line;
        break;
    }
}

if (empty($jsonLine)) {
    send_error('Inference script produced no JSON output. Exit code: ' . $exitCode . ', Details: ' . $rawOutput, "INFERENCE_FAILED", 500);
}

$result = json_decode($jsonLine, true);
if ($result === null) {
    send_error('Failed to parse inference JSON: ' . json_last_error_msg() . '. Raw: ' . $rawOutput, "PARSING_FAILED", 500);
}

if (isset($result['error'])) {
    send_error($result['error'], "INFERENCE_ERROR", 500);
}

// Forward result to frontend, merging standard fields and legacy fields for compatibility
http_response_code(200);
echo json_encode(array_merge([
    "success" => true,
    "status"  => "success",
    "message" => "Inference completed successfully.",
    "data"    => $result
], $result));
exit;
?>
