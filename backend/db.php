<?php
// db.php - Database connection helper
ini_set('display_errors', 0);
error_reporting(0);

// ─── CORS Headers ───────────────────────────────────────────────
// Allow the web browser (localhost:3000) to call this PHP backend.
// Android app is unaffected — it doesn't use CORS.
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With");

// Handle preflight OPTIONS request that browsers send before POST
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}
// ────────────────────────────────────────────────────────────────

header("Content-Type: application/json; charset=UTF-8");

$host = "localhost";
$username = "root";
$password = "";
$database = "nuerocheck_db";

$conn = @new mysqli($host, $username, $password, $database);
if ($conn->connect_error) {
    // Try fallback database names
    $conn = @new mysqli($host, $username, $password, "nuerocheck_web_db");
    if ($conn->connect_error) {
        $conn = @new mysqli($host, $username, $password, "nuerocheck_app_db");
    }
}

if ($conn->connect_error) {
    if (isset($ALLOW_NO_DB) && $ALLOW_NO_DB) {
        $conn = null;
    } else {
        http_response_code(500);
        echo json_encode([
            "success" => false,
            "status" => "error",
            "error" => [
                "code" => "DATABASE_CONNECTION_ERROR",
                "message" => "Database connection failed: " . $conn->connect_error
            ],
            "message" => "Database connection failed: " . $conn->connect_error
        ]);
        exit;
    }
}

// ── Centralized Response Formatters ───────────────────────────────

function send_success($data = [], $message = "Operation completed successfully", $code = 200) {
    http_response_code($code);
    echo json_encode([
        "success" => true,
        "status" => "success",
        "data" => $data,
        "message" => $message
    ]);
    exit;
}

function send_error($message, $error_code = "BAD_REQUEST", $http_code = 400) {
    http_response_code($http_code);
    echo json_encode([
        "success" => false,
        "status" => "error",
        "error" => [
            "code" => $error_code,
            "message" => $message
        ],
        "message" => $message
    ]);
    exit;
}
?>
