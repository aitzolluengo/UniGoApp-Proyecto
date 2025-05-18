<?php
require 'config.php';
header('Content-Type: application/json');

$userId = $_POST['user_id'] ?? null;
$paradaId = $_POST['parada_id'] ?? null;

if (!$userId || !$paradaId) {
    echo json_encode(["success" => false, "message" => "Parámetros inválidos"]);
    exit;
}

$db = connect();

$stmt = $db->prepare("INSERT IGNORE INTO favoritos (user_id, parada_id, fecha_agregado) VALUES (?, ?, NOW())");
$stmt->bind_param("is", $userId, $paradaId);

if ($stmt->execute()) {
    echo json_encode(["success" => true]);
} else {
    echo json_encode(["success" => false, "message" => "Error al insertar"]);
}
$stmt->close();
