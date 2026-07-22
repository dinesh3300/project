<?php
// smtp_auth_test.php - Full PHPMailer auth diagnostic
header('Content-Type: text/plain');

use PHPMailer\PHPMailer\PHPMailer;
use PHPMailer\PHPMailer\SMTP;
use PHPMailer\PHPMailer\Exception;

require_once __DIR__ . '/phpmailer/Exception.php';
require_once __DIR__ . '/phpmailer/PHPMailer.php';
require_once __DIR__ . '/phpmailer/SMTP.php';

$mail = new PHPMailer(true);
$debugLog = '';

try {
    $mail->isSMTP();
    $mail->Host       = 'smtp.gmail.com';
    $mail->SMTPAuth   = true;
    $mail->Username   = 'dineshkumarreddymaditati@gmail.com';
    $mail->Password   = 'nitsxutzylkuucsk';
    $mail->SMTPSecure = PHPMailer::ENCRYPTION_STARTTLS;
    $mail->Port       = 587;
    $mail->Timeout    = 10;   // 10 second timeout

    $mail->SMTPDebug = SMTP::DEBUG_SERVER;
    $mail->Debugoutput = function($str, $level) use (&$debugLog) {
        $debugLog .= "[Level $level] $str\n";
    };

    // Connect only (no email send)
    if ($mail->smtpConnect()) {
        echo "AUTH SUCCESS - SMTP authentication succeeded!\n\n";
        $mail->smtpClose();
    } else {
        echo "AUTH FAILED - Could not authenticate.\n\n";
    }
} catch (Exception $e) {
    echo "EXCEPTION: " . $e->getMessage() . "\n";
    echo "Error Info: " . $mail->ErrorInfo . "\n\n";
}

echo "=== SMTP Debug Log ===\n";
echo $debugLog;
