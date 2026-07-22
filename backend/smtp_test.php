<?php
// Quick SMTP connectivity test
header('Content-Type: text/plain');

$host = 'smtp.gmail.com';
$port = 587;
$timeout = 10;

$errno = 0;
$errstr = '';
$sock = @stream_socket_client("tcp://{$host}:{$port}", $errno, $errstr, $timeout);

if ($sock) {
    $banner = fgets($sock, 1024);
    fclose($sock);
    echo "SUCCESS - Connected to {$host}:{$port}\n";
    echo "Server banner: " . $banner . "\n";
} else {
    echo "FAILED - Cannot connect to {$host}:{$port}\n";
    echo "Error {$errno}: {$errstr}\n";
}

// Also test 465
$errno2 = 0; $errstr2 = '';
$sock2 = @stream_socket_client("ssl://{$host}:465", $errno2, $errstr2, $timeout);
if ($sock2) {
    $banner2 = fgets($sock2, 1024);
    fclose($sock2);
    echo "\nSUCCESS - Connected to {$host}:465 (SSL)\n";
    echo "Server banner: " . $banner2 . "\n";
} else {
    echo "\nFAILED - Cannot connect to {$host}:465 (SSL)\n";
    echo "Error {$errno2}: {$errstr2}\n";
}

// Test openssl
echo "\nOpenSSL version: " . OPENSSL_VERSION_TEXT . "\n";
echo "PHP version: " . phpversion() . "\n";
