<?php
require 'config.php';

if (!isset($_POST['email'], $_POST['password'])) {
    http_response_code(400);
    echo json_encode(["error"=>"Faltan campos"]);
    exit;
}

$db    = connect();
$email = $db->real_escape_string($_POST['email']);
$pass  = $_POST['password'];

$res = $db->query("SELECT * FROM users WHERE email='$email' LIMIT 1");
if (!$res || $res->num_rows===0) {
    http_response_code(401);
    echo json_encode(["error"=>"Credenciales incorrectas"]);
    exit;
}
$user = $res->fetch_assoc();
if (!password_verify($pass, $user['password'])) {
    http_response_code(401);
    echo json_encode(["error"=>"Credenciales incorrectas"]);
    exit;
}

// No envíes la password al cliente
unset($user['password']);
echo json_encode($user);
