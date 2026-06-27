CREATE DATABASE IF NOT EXISTS placetag CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE placetag;

CREATE TABLE IF NOT EXISTS places (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(120) NOT NULL,
    description TEXT NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    address VARCHAR(255) NULL,
    photoName VARCHAR(255) NULL,
    createdAt DATETIME NOT NULL
);
