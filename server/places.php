<?php
header('Content-Type: application/json; charset=utf-8');

$host = '127.0.0.1';
$db = 'placetag';
$user = 'root';
$password = '';

function respond(int $status, array $payload): void
{
    http_response_code($status);
    echo json_encode($payload, JSON_UNESCAPED_UNICODE);
    exit;
}

function readJsonBody(): array
{
    $data = json_decode(file_get_contents('php://input'), true);
    if (!is_array($data)) {
        respond(400, ['error' => 'Invalid JSON body']);
    }
    return $data;
}

function nullableText(array $data, string $key): ?string
{
    if (!array_key_exists($key, $data) || $data[$key] === null) {
        return null;
    }
    $value = trim((string)$data[$key]);
    return $value === '' ? null : $value;
}

function requiredText(array $data, string $key): string
{
    $value = nullableText($data, $key);
    if ($value === null) {
        respond(400, ['error' => "$key is required"]);
    }
    return $value;
}

function requiredFloat(array $data, string $key): float
{
    if (!array_key_exists($key, $data)) {
        respond(400, ['error' => "$key is required"]);
    }
    $value = filter_var($data[$key], FILTER_VALIDATE_FLOAT);
    if ($value === false) {
        respond(400, ['error' => "$key must be a number"]);
    }
    return (float)$value;
}

function normalizedCreatedAt(array $data): string
{
    $createdAt = nullableText($data, 'createdAt');
    if ($createdAt === null) {
        return date('Y-m-d H:i:s');
    }
    return str_replace('T', ' ', substr($createdAt, 0, 19));
}

try {
    $pdo = new PDO(
        "mysql:host=$host;dbname=$db;charset=utf8mb4",
        $user,
        $password,
        [
            PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
        ]
    );

    if ($_SERVER['REQUEST_METHOD'] === 'GET') {
        $stmt = $pdo->query('SELECT id AS remoteId, title, description, latitude, longitude, address, photoName, createdAt FROM places ORDER BY createdAt DESC');
        echo json_encode($stmt->fetchAll(), JSON_UNESCAPED_UNICODE);
        exit;
    }

    if ($_SERVER['REQUEST_METHOD'] === 'POST') {
        $data = readJsonBody();
        $title = requiredText($data, 'title');
        $description = nullableText($data, 'description');
        $latitude = requiredFloat($data, 'latitude');
        $longitude = requiredFloat($data, 'longitude');
        $address = nullableText($data, 'address');
        $photoName = nullableText($data, 'photoName');
        $createdAt = normalizedCreatedAt($data);
        $remoteId = isset($data['remoteId']) ? (int)$data['remoteId'] : 0;

        if ($remoteId > 0) {
            $stmt = $pdo->prepare(
                'UPDATE places SET title=?, description=?, latitude=?, longitude=?, address=?, photoName=?, createdAt=? WHERE id=?'
            );
            $stmt->execute([
                $title,
                $description,
                $latitude,
                $longitude,
                $address,
                $photoName,
                $createdAt,
                $remoteId,
            ]);
            echo json_encode(['remoteId' => $remoteId], JSON_UNESCAPED_UNICODE);
            exit;
        }

        $stmt = $pdo->prepare(
            'INSERT INTO places (title, description, latitude, longitude, address, photoName, createdAt) VALUES (?, ?, ?, ?, ?, ?, ?)'
        );
        $stmt->execute([
            $title,
            $description,
            $latitude,
            $longitude,
            $address,
            $photoName,
            $createdAt,
        ]);
        echo json_encode(['remoteId' => (int)$pdo->lastInsertId()], JSON_UNESCAPED_UNICODE);
        exit;
    }

    if ($_SERVER['REQUEST_METHOD'] === 'DELETE') {
        $id = isset($_GET['id']) ? (int)$_GET['id'] : 0;
        if ($id <= 0) {
            respond(400, ['error' => 'id is required']);
        }
        $stmt = $pdo->prepare('DELETE FROM places WHERE id=?');
        $stmt->execute([$id]);
        echo json_encode(['deleted' => $stmt->rowCount()], JSON_UNESCAPED_UNICODE);
        exit;
    }

    respond(405, ['error' => 'Method not allowed']);
} catch (Exception $e) {
    respond(500, ['error' => 'Server error', 'details' => $e->getMessage()]);
}
