<?php
require 'config.php';
header('Content-Type: application/json');

$userId = $_GET['user_id'] ?? null;

if (!$userId) {
    echo json_encode([]);
    exit;
}

$db = connect();

if (!$db) {
    echo json_encode([]);
    exit;
}

$stmt = $db->prepare("SELECT parada_id FROM favoritos WHERE user_id = ?");
$stmt->bind_param("i", $userId);
$stmt->execute();

$result = $stmt->get_result();
$favoritos = [];

while ($row = $result->fetch_assoc()) {
    $favoritos[] = $row['parada_id'];
}

echo json_encode($favoritos);
$stmt->close();
$db->close();
?>
