```sql
-- Run these in MySQL
CREATE DATABASE drink_order_system;
USE drink_order_system;

-- Tables (simplified example)
CREATE TABLE branches (id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(50));
CREATE TABLE drinks (id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(50), price DECIMAL(10,2), stock INT);
CREATE TABLE orders (id INT AUTO_INCREMENT PRIMARY KEY, customer_id INT, drink_id INT, branch_id INT, quantity INT, order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP);
CREATE TABLE stock_alerts (id INT AUTO_INCREMENT PRIMARY KEY, drink_name VARCHAR(50), branch_name VARCHAR(50), remaining_stock INT, is_shown BOOLEAN DEFAULT 0, alert_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP);

-- Insert branches
INSERT INTO branches (name) VALUES ('NAIROBI'), ('NAKURU'), ('MOMBASA'), ('KISUMU');
