<?php
require 'config.php';
if (!isset($_GET['id'])) {
    http_response_code(400);
    echo json_encode(["error"=>"Falta id"]);
    exit;
}
$db = connect();
$stmt = $db->prepare(
  "SELECT id,name,email,transportMode,darkMode FROM users WHERE id=?"
);
$stmt->bind_param("i", $_GET['id']);
$stmt->execute();
$res = $stmt->get_result();
if ($user=$res->fetch_assoc()) {
    echo json_encode($user);
} else {
    http_response_code(404);
    echo json_encode(["error"=>"No encontrado"]);
}
