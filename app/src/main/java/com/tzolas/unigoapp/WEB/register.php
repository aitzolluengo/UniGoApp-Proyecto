<?php
ini_set('display_errors', 1);
ini_set('display_startup_errors', 1);
error_reporting(E_ALL);
header('Content-Type: application/json; charset=UTF-8');

require 'config.php';

try {
    $db = connect();
    mysqli_report(MYSQLI_REPORT_ERROR | MYSQLI_REPORT_STRICT);
    if (!$db) {
        throw new Exception('Error al conectar a la base de datos');
    }

    // Recoger y validar
    $name  = trim($_POST['name']     ?? '');
    $email = trim($_POST['email']    ?? '');
    $pass  = trim($_POST['password'] ?? '');
    $mode  = trim($_POST['transportMode'] ?? 'walking');
    $dark  = isset($_POST['darkMode']) ? (int) $_POST['darkMode'] : 0;

    if ($name === '' || $email === '' || $pass === '') {
        http_response_code(400);
        echo json_encode(['success'=>false, 'message'=>'Faltan campos obligatorios']);
        exit;
    }

    // Escapar y validar email
    $nameEsc  = $db->real_escape_string($name);
    $emailEsc = $db->real_escape_string($email);
    if (!filter_var($emailEsc, FILTER_VALIDATE_EMAIL)) {
        http_response_code(400);
        echo json_encode(['success'=>false, 'message'=>'Email no válido']);
        exit;
    }
    $modeEsc = $db->real_escape_string($mode);

    // Verificar usuario
    $stmt = $db->prepare('SELECT id FROM users WHERE email = ?');
    $stmt->bind_param('s', $emailEsc);
    $stmt->execute();
    $stmt->store_result();
    if ($stmt->num_rows > 0) {
        http_response_code(409);
        echo json_encode(['success'=>false, 'message'=>'Usuario ya existe']);
        exit;
    }
    $stmt->close();

    // Insertar
    $hash = password_hash($pass, PASSWORD_DEFAULT);
    $stmt = $db->prepare('
        INSERT INTO users(name, email, password, transportMode, darkMode)
        VALUES (?, ?, ?, ?, ?)
    ');
    $stmt->bind_param('ssssi', $nameEsc, $emailEsc, $hash, $modeEsc, $dark);
    $stmt->execute();
    $stmt->close();

    http_response_code(201);
    echo json_encode(['success'=>true, 'message'=>'Registro exitoso']);

} catch (mysqli_sql_exception $e) {
    http_response_code(500);
    echo json_encode([
      'success'=>false,
      'message'=>'Error de base de datos',
      'error'=>$e->getMessage()
    ]);
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
      'success'=>false,
      'message'=>$e->getMessage()
    ]);
}
exit;
