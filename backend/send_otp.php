<?php
// send_otp.php - Generate and send OTP via PHPMailer (Gmail SMTP)
require_once 'db.php';

// ─────────────────────────────────────────────
// SMTP CONFIGURATION — Fill in your credentials
// Use a Gmail address with an App Password:
// Gmail → Settings → Security → 2-Step Verification → App Passwords
// ─────────────────────────────────────────────
define('SMTP_HOST',     'smtp.gmail.com');
define('SMTP_PORT',     587);
define('SMTP_USERNAME', 'dineshkumarreddymaditati@gmail.com');
define('SMTP_PASSWORD', 'nitsxutzylkuucsk');   // Nuerocheck App Password (no spaces)
define('SMTP_FROM',     'dineshkumarreddymaditati@gmail.com');
define('SMTP_FROM_NAME','Nuerocheck — Brain Hemorrhage Detection');

// ─────────────────────────────────────────────
// Load PHPMailer (no Composer — direct include)
// ─────────────────────────────────────────────
use PHPMailer\PHPMailer\PHPMailer;
use PHPMailer\PHPMailer\SMTP;
use PHPMailer\PHPMailer\Exception;

require_once __DIR__ . '/phpmailer/Exception.php';
require_once __DIR__ . '/phpmailer/PHPMailer.php';
require_once __DIR__ . '/phpmailer/SMTP.php';

// ─────────────────────────────────────────────
// Request validation
// ─────────────────────────────────────────────
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    send_error("Invalid request method. Only POST is allowed.", "METHOD_NOT_ALLOWED", 405);
}

$email  = isset($_POST['email'])  ? trim($_POST['email'])  : '';
$action = isset($_POST['action']) ? trim($_POST['action']) : '';

if (empty($email) || empty($action)) {
    send_error("Email and action parameters are required.", "VALIDATION_ERROR", 422);
}

$valid_actions = ['signup', 'forgot_pwd', 'update_pwd'];
if (!in_array($action, $valid_actions)) {
    send_error("Invalid action parameter.", "VALIDATION_ERROR", 422);
}

// For password-related actions, verify the email exists
if ($action === 'forgot_pwd' || $action === 'update_pwd') {
    $stmt = $conn->prepare("SELECT id FROM doctors WHERE email = ?");
    $stmt->bind_param("s", $email);
    $stmt->execute();
    $stmt->store_result();
    if ($stmt->num_rows === 0) {
        $stmt->close();
        send_error("No account found with this email address.", "NOT_FOUND", 404);
    }
    $stmt->close();
}

// For signup action, check that the email is NOT already registered
if ($action === 'signup') {
    $stmt = $conn->prepare("SELECT id FROM doctors WHERE email = ?");
    $stmt->bind_param("s", $email);
    $stmt->execute();
    $stmt->store_result();
    if ($stmt->num_rows > 0) {
        $stmt->close();
        $conn->close();
        send_error("Already Registered: An account with this email already exists. Please login instead.", "EMAIL_ALREADY_EXISTS", 409);
    }
    $stmt->close();
}


// ─────────────────────────────────────────────
// Clean up old OTPs and generate a new one
// ─────────────────────────────────────────────
// Only delete pending (unverified) OTPs. If the user already passed OTP
// verification (otp_code = 'VERIFIED') we must NOT delete that record,
// otherwise signup.php / reset_password.php will fail to find it.
$stmt = $conn->prepare(
    "DELETE FROM otp_verifications WHERE email = ? AND action = ? AND otp_code != 'VERIFIED'"
);
$stmt->bind_param("ss", $email, $action);
$stmt->execute();
$stmt->close();

$otp_code   = str_pad(mt_rand(0, 999999), 6, '0', STR_PAD_LEFT);

$stmt = $conn->prepare("INSERT INTO otp_verifications (email, otp_code, action, expires_at) VALUES (?, ?, ?, DATE_ADD(NOW(), INTERVAL 10 MINUTE))");
$stmt->bind_param("sss", $email, $otp_code, $action);

if (!$stmt->execute()) {
    $stmt->close();
    $conn->close();
    send_error("Failed to generate OTP. Please try again.", "INTERNAL_SERVER_ERROR", 500);
}
$stmt->close();

// Always log to file for fallback/debugging
$log_message = sprintf("[%s] Email: %s, Action: %s, OTP: %s\n",
    date('Y-m-d H:i:s'), $email, $action, $otp_code);
file_put_contents(__DIR__ . '/otp_log.txt', $log_message, FILE_APPEND);

// ─────────────────────────────────────────────
// Build email body
// ─────────────────────────────────────────────
$action_label = [
    'signup'      => 'Account Registration',
    'forgot_pwd'  => 'Password Reset',
    'update_pwd'  => 'Password Change',
][$action] ?? ucfirst($action);

$body = "
<html>
<body style='font-family: Arial, sans-serif; background: #f5f7fa; padding: 30px; margin: 0;'>
  <div style='max-width: 480px; margin: 0 auto; background: #ffffff; border-radius: 12px;
              box-shadow: 0 4px 20px rgba(0,0,0,0.08); overflow: hidden;'>
    <div style='background: linear-gradient(135deg, #1a3a6b 0%, #0d9488 100%);
                padding: 28px 32px; text-align: center;'>
      <h2 style='color: #ffffff; margin: 0; font-size: 20px; letter-spacing: 0.5px;'>
        Brain Hemorrhage Detection
      </h2>
      <p style='color: rgba(255,255,255,0.8); margin: 6px 0 0; font-size: 13px;'>
        Secure Verification Code
      </p>
    </div>
    <div style='padding: 32px;'>
      <p style='color: #374151; font-size: 15px; margin: 0 0 8px;'>
        Your OTP for <strong>{$action_label}</strong>:
      </p>
      <div style='background: #f0f4ff; border-radius: 10px; padding: 20px; text-align: center;
                  margin: 20px 0; border: 2px dashed #3b6ed4;'>
        <span style='font-size: 42px; font-weight: 900; letter-spacing: 16px; color: #1a3a6b;
                     font-family: \"Courier New\", monospace;'>{$otp_code}</span>
      </div>
      <p style='color: #6b7280; font-size: 13px; text-align: center; margin: 0;'>
        ⏰ This code expires in <strong>10 minutes</strong>.
      </p>
      <p style='color: #9ca3af; font-size: 12px; text-align: center; margin: 16px 0 0;'>
        If you did not request this, please ignore this email.
      </p>
    </div>
    <div style='background: #f9fafb; padding: 16px 32px; text-align: center;
                border-top: 1px solid #e5e7eb;'>
      <p style='color: #d1d5db; font-size: 11px; margin: 0;'>
        © Nuerocheck AI Diagnostics — Confidential
      </p>
    </div>
  </div>
</body>
</html>";

// ─────────────────────────────────────────────
// ASYNC EMAIL SEND — respond to client first, then send email in background
// This prevents Retrofit/fetch 30-second timeouts when Gmail SMTP is slow.
// ─────────────────────────────────────────────

// ── Step 1: Send HTTP response immediately ────────────────────────────────────
// Flush the "OTP sent" success response to the Android app / web browser NOW,
// before the slow SMTP handshake happens. The email will still be delivered.
$conn->close();

// Tell PHP to keep running even after the client disconnects / response is sent
ignore_user_abort(true);
set_time_limit(120);   // Allow up to 2 minutes for the background SMTP send

// Buffer and flush the success response to the client right away
if (ob_get_level() === 0) ob_start();
http_response_code(200);
echo json_encode([
    "success" => true,
    "status"  => "success",
    "data"    => [],
    "message" => "A 6-digit verification code has been sent to {$email}. Please check your inbox."
]);

// Flush to client immediately (works on Apache/XAMPP with mod_php)
$response_length = ob_get_length();
header("Content-Length: {$response_length}");
header("Connection: close");
ob_end_flush();
flush();

// ── Step 2: Send email in the background (client already received the response) ─
$mail = new PHPMailer(true);
$debugLog = '';

try {
    $mail->isSMTP();
    $mail->Host       = SMTP_HOST;
    $mail->SMTPAuth   = true;
    $mail->Username   = SMTP_USERNAME;
    $mail->Password   = SMTP_PASSWORD;
    $mail->SMTPSecure = PHPMailer::ENCRYPTION_STARTTLS;
    $mail->Port       = SMTP_PORT;
    $mail->Timeout    = 60;   // Generous timeout since we're in the background now

    // Capture debug output to log file (not browser — response already sent)
    $mail->SMTPDebug  = 0;   // Disable debug output in background mode

    $mail->setFrom(SMTP_FROM, SMTP_FROM_NAME);
    $mail->addAddress($email);
    $mail->addReplyTo(SMTP_FROM, SMTP_FROM_NAME);

    $mail->isHTML(true);
    $mail->Subject = "[Nuerocheck] Your OTP Code — {$otp_code}";
    $mail->Body    = $body;
    $mail->AltBody = "Your {$action_label} OTP code is: {$otp_code}. It expires in 10 minutes.";

    $mail->send();

    file_put_contents(__DIR__ . '/otp_log.txt',
        sprintf("[%s] [SENT] Email: %s, Action: %s, OTP: %s\n",
            date('Y-m-d H:i:s'), $email, $action, $otp_code),
        FILE_APPEND);

} catch (Exception $e) {
    // Log SMTP failure — the client already got a success response so we cannot
    // change the HTTP status, but future retries via "Resend OTP" will try again.
    file_put_contents(__DIR__ . '/otp_log.txt',
        sprintf("[%s] SMTP ERROR for %s: %s\n",
            date('Y-m-d H:i:s'), $email, $mail->ErrorInfo),
        FILE_APPEND);
}
?>
