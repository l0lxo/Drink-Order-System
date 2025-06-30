import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AdminUI {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/drink_order_system";
    private static final String USER = "root";
    private static final String PASS = "grace";
    private static int stockThreshold = 5;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(AdminUI::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Admin Stock Management");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 700);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Stock Management", createStockPanel());
        tabbedPane.addTab("Reports", createReportsPanel());
        tabbedPane.addTab("Alerts", createAlertsPanel());

        frame.add(tabbedPane);
        frame.setVisible(true);
    }

    // Stock Management Panel
    private static JPanel createStockPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Input fields
        JPanel inputPanel = new JPanel(new GridLayout(5, 2, 5, 5));
        JTextField drinkField = new JTextField();
        JTextField brandField = new JTextField();
        JTextField priceField = new JTextField();
        JTextField quantityField = new JTextField();
        JComboBox<String> branchCombo = new JComboBox<>(new String[]{"NAIROBI", "NAKURU", "MOMBASA", "KISUMU"});

        inputPanel.add(new JLabel("Drink Name:"));
        inputPanel.add(drinkField);
        inputPanel.add(new JLabel("Brand:"));
        inputPanel.add(brandField);
        inputPanel.add(new JLabel("Price (KES):"));
        inputPanel.add(priceField);
        inputPanel.add(new JLabel("Quantity:"));
        inputPanel.add(quantityField);
        inputPanel.add(new JLabel("Branch:"));
        inputPanel.add(branchCombo);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton addButton = new JButton("Add/Update Stock");
        JButton viewButton = new JButton("View Current Stock");
        buttonPanel.add(addButton);
        buttonPanel.add(viewButton);

        // Output area
        JTextArea outputArea = new JTextArea(15, 50);
        outputArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(outputArea);

        // Add action listeners
        addButton.addActionListener(e -> handleAddStock(drinkField, brandField, priceField, quantityField, branchCombo, outputArea));
        viewButton.addActionListener(e -> handleViewStock(outputArea));

        // Assemble panel
        panel.add(inputPanel, BorderLayout.NORTH);
        panel.add(buttonPanel, BorderLayout.CENTER);
        panel.add(scrollPane, BorderLayout.SOUTH);

        return panel;
    }

    private static void handleAddStock(JTextField drinkField, JTextField brandField, JTextField priceField,
                                       JTextField quantityField, JComboBox<String> branchCombo, JTextArea outputArea) {
        try {
            String drink = drinkField.getText().trim();
            String brand = brandField.getText().trim();
            double price = Double.parseDouble(priceField.getText());
            int quantity = Integer.parseInt(quantityField.getText());
            String branch = (String) branchCombo.getSelectedItem();

            try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
                // Check if drink exists
                PreparedStatement checkStmt = conn.prepareStatement(
                        "SELECT id FROM drinks WHERE LOWER(name) = LOWER(?) AND LOWER(brand) = LOWER(?)");
                checkStmt.setString(1, drink);
                checkStmt.setString(2, brand);
                ResultSet rs = checkStmt.executeQuery();

                if (rs.next()) {
                    // Update existing
                    PreparedStatement updateStmt = conn.prepareStatement(
                            "UPDATE drinks SET price = ?, stock = stock + ? WHERE id = ?");
                    updateStmt.setDouble(1, price);
                    updateStmt.setInt(2, quantity);
                    updateStmt.setInt(3, rs.getInt("id"));
                    updateStmt.executeUpdate();
                    outputArea.setText("Updated existing stock for " + drink);
                } else {
                    // Add new
                    PreparedStatement insertStmt = conn.prepareStatement(
                            "INSERT INTO drinks (name, brand, price, stock) VALUES (?, ?, ?, ?)");
                    insertStmt.setString(1, drink);
                    insertStmt.setString(2, brand);
                    insertStmt.setDouble(3, price);
                    insertStmt.setInt(4, quantity);
                    insertStmt.executeUpdate();
                    outputArea.setText("Added new drink: " + drink);
                }

                // Log restock
                PreparedStatement logStmt = conn.prepareStatement(
                        "INSERT INTO restock_history (drink_name, quantity_added, branch_name) VALUES (?, ?, ?)");
                logStmt.setString(1, drink);
                logStmt.setInt(2, quantity);
                logStmt.setString(3, branch);
                logStmt.executeUpdate();

            } catch (SQLException ex) {
                outputArea.setText("Database error: " + ex.getMessage());
            }
        } catch (NumberFormatException ex) {
            outputArea.setText("Please enter valid numbers for price and quantity");
        }
    }

    private static void handleViewStock(JTextArea outputArea) {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT name, brand, price, SUM(stock) as total_stock FROM drinks GROUP BY name, brand, price")) {

            StringBuilder sb = new StringBuilder("Current Stock:\n\n");
            while (rs.next()) {
                sb.append(String.format("%s (%s) - KES %,.2f - %d units\n",
                        rs.getString("name"),
                        rs.getString("brand"),
                        rs.getDouble("price"),
                        rs.getInt("total_stock")));
            }
            outputArea.setText(sb.toString());
        } catch (SQLException ex) {
            outputArea.setText("Error loading stock: " + ex.getMessage());
        }
    }

    // Reports Panel
    private static JPanel createReportsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTextArea reportArea = new JTextArea(15, 50);
        reportArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(reportArea);

        JPanel buttonPanel = new JPanel(new GridLayout(1, 4, 5, 5));

        JButton salesReportBtn = new JButton("Sales Report");
        JButton customerReportBtn = new JButton("Customer Orders");
        JButton businessReportBtn = new JButton("Business Summary");
        JButton revenueReportBtn = new JButton("Total Revenue"); // New button

        salesReportBtn.addActionListener(e -> generateSalesReport(reportArea));
        customerReportBtn.addActionListener(e -> generateCustomerReport(reportArea));
        businessReportBtn.addActionListener(e -> generateBusinessSummary(reportArea));
        revenueReportBtn.addActionListener(e -> generateTotalRevenueReport(reportArea)); // New action

        buttonPanel.add(salesReportBtn);
        buttonPanel.add(customerReportBtn);
        buttonPanel.add(businessReportBtn);
        buttonPanel.add(revenueReportBtn); // Add new button

        panel.add(buttonPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private static void generateSalesReport(JTextArea reportArea) {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            StringBuilder report = new StringBuilder("=== SALES REPORT ===\n\n");

            // 1. By Branch (validates requirement: Branches where orders were made)
            report.append("--- Sales By Branch ---\n");
            PreparedStatement branchStmt = conn.prepareStatement(
                    "SELECT b.name, COUNT(o.id) as order_count, " +
                            "SUM(d.price * o.quantity) as total_sales " +
                            "FROM orders o " +
                            "JOIN branches b ON o.branch_id = b.id " +
                            "JOIN drinks d ON o.drink_id = d.id " +
                            "GROUP BY b.name ORDER BY total_sales DESC");

            ResultSet rs = branchStmt.executeQuery();
            double grandTotal = 0;
            while (rs.next()) {
                double branchTotal = rs.getDouble("total_sales");
                report.append(String.format("%-10s: %d orders (KES %,.2f)\n",
                        rs.getString("name"),
                        rs.getInt("order_count"),
                        branchTotal));
                grandTotal += branchTotal;
            }
            report.append(String.format("\nTOTAL: KES %,.2f\n\n", grandTotal));

            // 2. By Customer (validates requirement: Customers who made orders)
            report.append("--- Top Customers ---\n");
            PreparedStatement customerStmt = conn.prepareStatement(
                    "SELECT c.name, COUNT(o.id) as order_count, " +
                            "SUM(d.price * o.quantity) as total_spent " +
                            "FROM orders o " +
                            "JOIN customers c ON o.customer_id = c.id " +
                            "JOIN drinks d ON o.drink_id = d.id " +
                            "GROUP BY c.name ORDER BY total_spent DESC LIMIT 10");

            rs = customerStmt.executeQuery();
            while (rs.next()) {
                report.append(String.format("%-20s: %d orders (KES %,.2f)\n",
                        rs.getString("name"),
                        rs.getInt("order_count"),
                        rs.getDouble("total_spent")));
            }

            reportArea.setText(report.toString());
        } catch (SQLException ex) {
            reportArea.setText("Error generating sales report: " + ex.getMessage());
        }
    }

    private static void generateCustomerReport(JTextArea reportArea) {
        String customerName = JOptionPane.showInputDialog("Enter customer name:");
        if (customerName == null || customerName.trim().isEmpty()) return;

        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            StringBuilder report = new StringBuilder("=== CUSTOMER ORDER HISTORY ===\n\n");
            report.append("Customer: ").append(customerName).append("\n\n");

            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT o.order_date, d.name as drink, o.quantity, d.price, b.name as branch " +
                            "FROM orders o JOIN drinks d ON o.drink_id = d.id " +
                            "JOIN branches b ON o.branch_id = b.id " +
                            "JOIN customers c ON o.customer_id = c.id " +
                            "WHERE c.name = ? ORDER BY o.order_date DESC");
            stmt.setString(1, customerName);
            ResultSet rs = stmt.executeQuery();

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            double totalSpent = 0;
            int orderCount = 0;

            while (rs.next()) {
                orderCount++;
                Date orderDate = rs.getTimestamp("order_date");
                String drink = rs.getString("drink");
                int quantity = rs.getInt("quantity");
                double price = rs.getDouble("price");
                String branch = rs.getString("branch");
                double orderTotal = price * quantity;
                totalSpent += orderTotal;

                report.append(String.format("[%s] %s @ %s\n  %d x KES %,.2f = KES %,.2f\n\n",
                        sdf.format(orderDate),
                        drink,
                        branch,
                        quantity,
                        price,
                        orderTotal));
            }

            report.append(String.format("TOTAL ORDERS: %d\nTOTAL SPENT: KES %,.2f",
                    orderCount, totalSpent));
            reportArea.setText(report.toString());
        } catch (SQLException ex) {
            reportArea.setText("Error generating customer report: " + ex.getMessage());
        }
    }

    private static void generateBusinessSummary(JTextArea reportArea) {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            StringBuilder report = new StringBuilder("=== BUSINESS SUMMARY ===\n\n");

            // 1. Overall stats (validates requirement: Total business revenue)
            PreparedStatement statsStmt = conn.prepareStatement(
                    "SELECT COUNT(DISTINCT o.id) as orders, " +
                            "COUNT(DISTINCT c.id) as customers, " +
                            "SUM(d.price * o.quantity) as revenue " +
                            "FROM orders o " +
                            "JOIN drinks d ON o.drink_id = d.id " +
                            "JOIN customers c ON o.customer_id = c.id");

            ResultSet rs = statsStmt.executeQuery();
            if (rs.next()) {
                report.append(String.format("Total Orders: %d\n", rs.getInt("orders")));
                report.append(String.format("Total Customers: %d\n", rs.getInt("customers")));
                report.append(String.format("Total Revenue: KES %,.2f\n\n", rs.getDouble("revenue")));
            }

            // 2. Branch comparison (validates requirement: Sales per branch)
            report.append("--- Branch Performance ---\n");
            PreparedStatement branchStmt = conn.prepareStatement(
                    "SELECT b.name, COUNT(o.id) as orders, " +
                            "SUM(d.price * o.quantity) as revenue " +
                            "FROM orders o " +
                            "JOIN branches b ON o.branch_id = b.id " +
                            "JOIN drinks d ON o.drink_id = d.id " +
                            "GROUP BY b.name ORDER BY revenue DESC");

            rs = branchStmt.executeQuery();
            while (rs.next()) {
                report.append(String.format("%-10s: %d orders (KES %,.2f)\n",
                        rs.getString("name"),
                        rs.getInt("orders"),
                        rs.getDouble("revenue")));
            }

            reportArea.setText(report.toString());
        } catch (SQLException ex) {
            reportArea.setText("Error generating business summary: " + ex.getMessage());
        }
    }

    // Alerts Panel
    private static JPanel createAlertsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTextArea alertsArea = new JTextArea(15, 50);
        alertsArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(alertsArea);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton refreshBtn = new JButton("Refresh Alerts");
        JButton clearBtn = new JButton("Clear Alerts");

        refreshBtn.addActionListener(e -> loadAlerts(alertsArea));
        clearBtn.addActionListener(e -> clearAlerts(alertsArea));

        buttonPanel.add(refreshBtn);
        buttonPanel.add(clearBtn);

        panel.add(buttonPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Load initial alerts
        loadAlerts(alertsArea);

        return panel;
    }

    private static void loadAlerts(JTextArea alertsArea) {
        checkForLowStockAlerts(); // Check for new alerts first

        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT * FROM stock_alerts ORDER BY alert_time DESC")) {

            StringBuilder sb = new StringBuilder("=== STOCK ALERTS ===\n\n");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

            if (!rs.isBeforeFirst()) {
                sb.append("No active alerts\n");
            } else {
                while (rs.next()) {
                    sb.append(String.format("[%s] %s @ %s\n  Remaining: %d units\n\n",
                            sdf.format(rs.getTimestamp("alert_time")),
                            rs.getString("drink_name"),
                            rs.getString("branch_name"),
                            rs.getInt("remaining_stock")));
                }
            }

            alertsArea.setText(sb.toString());
        } catch (SQLException ex) {
            alertsArea.setText("Error loading alerts: " + ex.getMessage());
        }
    }

    private static void clearAlerts(JTextArea alertsArea) {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM stock_alerts");
            alertsArea.setText("All alerts cleared successfully");
        } catch (SQLException ex) {
            alertsArea.setText("Error clearing alerts: " + ex.getMessage());
        }
    }
    private static void generateTotalRevenueReport(JTextArea reportArea) {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            StringBuilder report = new StringBuilder("=== TOTAL BUSINESS REVENUE ===\n\n");

            // Overall revenue (calculated from orders and drinks)
            PreparedStatement totalStmt = conn.prepareStatement(
                    "SELECT SUM(d.price * o.quantity) as total_revenue " +
                            "FROM orders o " +
                            "JOIN drinks d ON o.drink_id = d.id");

            ResultSet rs = totalStmt.executeQuery();
            if (rs.next()) {
                report.append(String.format("Total Revenue: KES %,.2f\n\n", rs.getDouble("total_revenue")));
            }

            // Revenue by branch (calculated from orders and drinks)
            report.append("--- Revenue by Branch ---\n");
            PreparedStatement branchStmt = conn.prepareStatement(
                    "SELECT b.name, SUM(d.price * o.quantity) as branch_revenue " +
                            "FROM orders o " +
                            "JOIN drinks d ON o.drink_id = d.id " +
                            "JOIN branches b ON o.branch_id = b.id " +
                            "GROUP BY b.name " +
                            "ORDER BY branch_revenue DESC");

            rs = branchStmt.executeQuery();
            while (rs.next()) {
                report.append(String.format("%-10s: KES %,.2f\n",
                        rs.getString("name"),
                        rs.getDouble("branch_revenue")));
            }

            reportArea.setText(report.toString());
        } catch (SQLException ex) {
            reportArea.setText("Error generating revenue report: " + ex.getMessage());
        }
    }
    private static void checkForLowStockAlerts() {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT drink_name, branch_name, remaining_stock " +
                            "FROM stock_alerts " +
                            "WHERE is_shown = 0"); // Only show unseen alerts

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String drink = rs.getString("drink_name");
                String branch = rs.getString("branch_name");
                int remainingStock = rs.getInt("remaining_stock");

                // Show pop-up alert
                JOptionPane.showMessageDialog(
                        null,
                        "⚠️ LOW STOCK ALERT ⚠️\n" +
                                "Drink: " + drink + "\n" +
                                "Branch: " + branch + "\n" +
                                "Remaining: " + remainingStock + " units",
                        "Stock Alert",
                        JOptionPane.WARNING_MESSAGE
                );

                // Mark alert as shown
                PreparedStatement updateStmt = conn.prepareStatement(
                        "UPDATE stock_alerts SET is_shown = 1 " +
                                "WHERE drink_name = ? AND branch_name = ?");
                updateStmt.setString(1, drink);
                updateStmt.setString(2, branch);
                updateStmt.executeUpdate();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
}
