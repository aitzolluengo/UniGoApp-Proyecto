<?php
require 'config.php';

if (!isset($_POST['name'], $_POST['email'], $_POST['password'])) {
    http_response_code(400);
    echo json_encode(["success"=>false, "message"=>"Faltan campos"]);
    exit;
}

$db    = connect();
$name  = $db->real_escape_string($_POST['name']);
$email = $db->real_escape_string($_POST['email']);
$pass  = password_hash($_POST['password'], PASSWORD_DEFAULT);
$mode  = $db->real_escape_string($_POST['transportMode'] ?? 'walking');
$dark  = isset($_POST['darkMode']) ? (int)$_POST['darkMode'] : 0;

// Comprueba que no exista
$res = $db->query("SELECT id FROM users WHERE email='$email'");
if ($res && $res->num_rows>0) {
    echo json_encode(["success"=>false, "message"=>"Usuario ya existe"]);
    exit;
}

$stmt = $db->prepare(
  "INSERT INTO users(name,email,password,transportMode,darkMode)
   VALUES(?,?,?,?,?)"
);
$stmt->bind_param("ssssi", $name, $email, $pass, $mode, $dark);

if ($stmt->execute()) {
    echo json_encode(["success"=>true, "message"=>"Registro exitoso"]);
} else {
    http_response_code(500);
    echo json_encode(["success"=>false, "message"=>"Error interno"]);
}
