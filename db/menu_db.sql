-- Import bằng phpMyAdmin (XAMPP) nếu muốn tạo DB thủ công.
-- (Server cũng tự tạo DB + bảng + dữ liệu mẫu khi khởi động nên bước này KHÔNG bắt buộc.)
CREATE DATABASE IF NOT EXISTS menu_db CHARACTER SET utf8 COLLATE utf8_unicode_ci;
USE menu_db;

CREATE TABLE IF NOT EXISTS category (
  id VARCHAR(20) PRIMARY KEY,
  name VARCHAR(100) NOT NULL UNIQUE,
  description VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS dish (
  id VARCHAR(20) PRIMARY KEY,
  category_id VARCHAR(20) NOT NULL,
  name VARCHAR(100) NOT NULL,
  image VARCHAR(255),
  price DECIMAL(15,0) NOT NULL,
  serve_time INT NOT NULL,
  FOREIGN KEY (category_id) REFERENCES category(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
