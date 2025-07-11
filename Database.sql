Other branches:
CREATE DATABASE drink_order_system;
USE drink_order_system;

-- Tables (simplified example)
CREATE TABLE branches (id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(50));
CREATE TABLE drinks (id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(50), price DECIMAL(10,2), stock INT);
CREATE TABLE orders (id INT AUTO_INCREMENT PRIMARY KEY, customer_id INT, drink_id INT, branch_id INT, quantity INT, order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP);
CREATE TABLE stock_alerts (id INT AUTO_INCREMENT PRIMARY KEY, drink_name VARCHAR(50), branch_name VARCHAR(50), remaining_stock INT, is_shown BOOLEAN DEFAULT 0, alert_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP);

-- Insert branches
INSERT INTO branches (name) VALUES ('NAIROBI'), ('NAKURU'), ('MOMBASA'), ('KISUMU');

Main branch:
-- Core Tables
CREATE TABLE branches (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL
);

CREATE TABLE customers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);

CREATE TABLE drinks (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    brand VARCHAR(50),
    price DECIMAL(10,2) NOT NULL,
    stock INT NOT NULL,
    branch_id INT,
    FOREIGN KEY (branch_id) REFERENCES branches(id)
);

CREATE TABLE orders (
    id INT AUTO_INCREMENT PRIMARY KEY,
    customer_id INT NOT NULL,
    drink_id INT NOT NULL,
    branch_id INT NOT NULL,
    quantity INT NOT NULL,
    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(id),
    FOREIGN KEY (drink_id) REFERENCES drinks(id),
    FOREIGN KEY (branch_id) REFERENCES branches(id)
);

CREATE TABLE payments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL,
    method ENUM('M-Pesa', 'Card', 'Cash') NOT NULL,
    status VARCHAR(20) DEFAULT 'Pending',
    receipt VARCHAR(100),
    amount DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(id)
);

-- Alert System
CREATE TABLE stock_alerts (
    id INT AUTO_INCREMENT PRIMARY KEY,
    drink_name VARCHAR(50) NOT NULL,
    branch_name VARCHAR(50) NOT NULL,
    remaining_stock INT NOT NULL,
    is_shown BOOLEAN DEFAULT 0,
    alert_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Optional Logs (Referenced in AdminUI.java)
CREATE TABLE restock_history (
    id INT AUTO_INCREMENT PRIMARY KEY,
    drink_name VARCHAR(50) NOT NULL,
    quantity_added INT NOT NULL,
    branch_name VARCHAR(50) NOT NULL,
    restock_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE stock_redistribution_log (
    id INT AUTO_INCREMENT PRIMARY KEY,
    drink_name VARCHAR(50) NOT NULL,
    from_branch VARCHAR(50) NOT NULL,
    to_branch VARCHAR(50) NOT NULL,
    quantity INT NOT NULL,
    redistribution_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Configuration
CREATE TABLE settings (
    setting_key VARCHAR(50) PRIMARY KEY,
    setting_value VARCHAR(100)
);

-- Insert default threshold
INSERT INTO settings (setting_key, setting_value) VALUES ('stock_threshold', '5');

-- Insert branches (HQ + 3 branches)
INSERT INTO branches (name) VALUES 
    ('NAIROBI'), 
    ('NAKURU'), 
    ('MOMBASA'), 
    ('KISUMU');

Incase of an error in the drinks table, here the solution is:
-- Add the column at the end first
ALTER TABLE drinks ADD COLUMN brand VARCHAR(50) AFTER name;

-- Then modify the column position (MySQL doesn't support direct position change)
ALTER TABLE drinks MODIFY COLUMN brand VARCHAR(50) AFTER name;

You can now add the drinks. Here is a sample we used:
-- Insert sample drinks data into the drinks table
INSERT INTO drinks (id, name, brand, price, stock) VALUES
(1, 'Coke', 'CocaCola', 100.00, 99),
(2, 'Sprite', 'CocaCola', 95.00, 40),
(3, 'Fanta', 'CocaCola', 90.00, 30),
(4, 'Pepsi', 'PepsiCo', 85.00, 22),
(5, 'Cola', 'CocaCola', 100.00, 50),
(6, 'Dr Pepper', 'Pepsi', 190.00, 147);

-- If you need to reset auto-increment after manual ID insertion
ALTER TABLE drinks AUTO_INCREMENT = 7;
