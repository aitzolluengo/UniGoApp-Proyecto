<?php
header("Content-Type: application/json; charset=UTF-8");

define('DB_HOST', 'localhost');
define('DB_USER', 'Xxbadiola002');
define('DB_PASS', 'M11BQsUmeQ');
define('DB_NAME', 'Xxbadiola002_unigoapp ');

function connect() {
    $mysqli = new mysqli(DB_HOST, DB_USER, DB_PASS, DB_NAME);
    if ($mysqli->connect_error) {
        http_response_code(500);
        echo json_encode(["error" => $mysqli->connect_error]);
        exit;
    }
    $mysqli->set_charset("utf8");
    return $mysqli;
}
