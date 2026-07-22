<?php
$host = "localhost";
$username = "root";
$password = ""; // Default XAMPP password is empty
$database = "brain_scan_db";

$conn = new mysqli($host, $username, $password, $database);

if ($conn->connect_error) {
    header('Content-Type: application/json');
    die(json_encode(["status" => "error", "message" => "Connection failed: " . $conn->connect_error]));
}

  