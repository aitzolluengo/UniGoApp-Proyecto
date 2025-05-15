<?php
require 'config.php';
$data = $_POST; // o json_decode(file_get_contents('php://input'), true);
if (!isset($data['id'])) {
    http_response_code(400);
    echo json_encode(["error"=>"Falta id"]);
    exit;
}
$db = connect();
$stmt = $db->prepare(
  "UPDATE users SET transportMode=?, darkMode=? WHERE id=?"
);
$stmt->bind_param(
  "sii",
  $data['transportMode'] ?? 'walking',
  $data['darkMode'] ?? 0,
  $data['id']
);
if ($stmt->execute()) {
    echo json_encode(["success"=>true]);
} else {
    http_response_code(500);
    echo json_encode(["error"=>"No se pudo actualizar"]);
}
