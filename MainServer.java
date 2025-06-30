import java.io.*;
import java.net.*;
import java.sql.*;
import java.nio.file.*;
import java.util.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainServer {
    private static final int PORT = 5000;
    private static final String DB_URL = "jdbc:mysql://localhost:3306/drink_order_system";
    private static final String USER = "root";
    private static final String PASS = "grace";
    private static final Map<String, List<Order>> branchOrders = new HashMap<>();
    private static int stockThreshold = 5;

    public static void main(String[] args) {
        loadSettings();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);
            logToFile("SERVER STARTED", "Main server initialized");

            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(() -> handleClient(socket)).start();
            }
        } catch (IOException e) {
            logError("Server startup failed", e);
        }
    }

    private static void loadSettings() {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT setting_value FROM settings WHERE setting_key = 'stock_threshold'")) {

            if (rs.next()) {
                stockThreshold = Integer.parseInt(rs.getString("setting_value"));
            }
        } catch (SQLException | NumberFormatException e) {
            logError("Failed to load settings", e);
        }
    }

    private static void handleClient(Socket socket) {
        try (
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream())
        ) {
            Order order = (Order) in.readObject();
            System.out.println("Processing order for: " + order.getCustomer().getName());

            try {
                // Save order to DB
                processSuccessfulOrder(order, out);
            } catch (SQLException | IOException e) {
                logError("Processing order failed", e);
                out.writeObject("Order failed: " + e.getMessage());
            }

        } catch (Exception e) {
            e.printStackTrace(); // log full exception for debug
        }
    }


    private static void processSuccessfulOrder(Order order, ObjectOutputStream out) throws SQLException, IOException {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            conn.setAutoCommit(false);

            // 1. Update stock
            updateStock(conn, order);

            // 2. Save order and payment
            int orderId = saveOrder(conn, order);
            savePayment(conn, orderId, order.getPayment());

            // 3. Check stock levels and redistribute if needed
            checkStockLevels(conn, order);

            conn.commit();

            // 4. Prepare success response
            String response = String.format(
                    "Order #%d completed. Receipt: %s. Total: KES %,.2f",
                    orderId,
                    order.getPayment().getReceipt(),
                    order.getTotalPrice()
            );

            out.writeObject(response);
            logToFile("ORDER COMPLETED", "ID: " + orderId + " | " + response);
        }
    }

    private static void updateStock(Connection conn, Order order) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(
                "UPDATE drinks SET stock = stock - ? WHERE name = ? AND stock >= ?");
        ps.setInt(1, order.getQuantity());
        ps.setString(2, order.getDrink().getName());
        ps.setInt(3, order.getQuantity());

        if (ps.executeUpdate() == 0) {
            throw new SQLException("Insufficient stock for " + order.getDrink().getName());
        }
    }

    private static int saveOrder(Connection conn, Order order) throws SQLException {
        int customerId = getOrCreateCustomer(conn, order.getCustomer().getName());

        PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO orders (customer_id, drink_id, branch_id, quantity) " +
                        "VALUES (?, (SELECT id FROM drinks WHERE name = ?), " +
                        "(SELECT id FROM branches WHERE name = ?), ?)",
                Statement.RETURN_GENERATED_KEYS);

        ps.setInt(1, customerId);
        ps.setString(2, order.getDrink().getName());
        ps.setString(3, order.getBranchName());
        ps.setInt(4, order.getQuantity());
        ps.executeUpdate();

        ResultSet rs = ps.getGeneratedKeys();
        return rs.next() ? rs.getInt(1) : -1;
    }

    private static void savePayment(Connection conn, int orderId, Payment payment) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO payments (order_id, method, status, receipt, amount) " +
                        "VALUES (?, ?, ?, ?, ?)");
        ps.setInt(1, orderId);
        ps.setString(2, payment.getMethod());
        ps.setString(3, payment.getStatus());
        ps.setString(4, payment.getReceipt());
        ps.setDouble(5, payment.getAmount());
        ps.executeUpdate();
    }

    private static int getOrCreateCustomer(Connection conn, String name) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(
                "INSERT IGNORE INTO customers (name) VALUES (?)",
                Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, name);
        ps.executeUpdate();

        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) return rs.getInt(1);

        ps = conn.prepareStatement("SELECT id FROM customers WHERE name = ?");
        ps.setString(1, name);
        rs = ps.executeQuery();
        return rs.next() ? rs.getInt(1) : -1;
    }

    private static void checkStockLevels(Connection conn, Order order) throws SQLException {

        // Original alert logging (keep this)
        PreparedStatement ps = conn.prepareStatement(
                "SELECT stock FROM drinks WHERE name = ?");
        ps.setString(1, order.getDrink().getName());

        ResultSet rs = ps.executeQuery();
        if (rs.next() && rs.getInt("stock") < stockThreshold) {
            PreparedStatement alert = conn.prepareStatement(
                    "INSERT INTO stock_alerts (branch_name, drink_name, remaining_stock) " +
                            "VALUES (?, ?, ?)");
            alert.setString(1, order.getBranchName());
            alert.setString(2, order.getDrink().getName());
            alert.setInt(3, rs.getInt("stock"));
            alert.executeUpdate();
        }
    }

    private static void checkForRedistribution(Connection conn, String drinkName, String branchName) throws SQLException {
        if (branchName.equals("NAIROBI")) return;  // HQ doesn't need redistribution

        // Get current stock at branch
        PreparedStatement branchStmt = conn.prepareStatement(
                "SELECT stock FROM drinks WHERE name = ? AND branch_id = " +
                        "(SELECT id FROM branches WHERE name = ?)");
        branchStmt.setString(1, drinkName);
        branchStmt.setString(2, branchName);
        ResultSet branchRs = branchStmt.executeQuery();

        if (branchRs.next() && branchRs.getInt("stock") < stockThreshold) {
            int needed = stockThreshold - branchRs.getInt("stock");

            // Get HQ stock
            PreparedStatement hqStmt = conn.prepareStatement(
                    "SELECT stock FROM drinks WHERE name = ? AND branch_id = " +
                            "(SELECT id FROM branches WHERE name = 'NAIROBI') FOR UPDATE");
            hqStmt.setString(1, drinkName);
            ResultSet hqRs = hqStmt.executeQuery();

            if (hqRs.next() && hqRs.getInt("stock") > needed) {
                // Perform redistribution
                redistributeStock(conn, drinkName, branchName, needed);
            }
        }
    }

    private static void redistributeStock(Connection conn, String drinkName, String branchName, int quantity) throws SQLException {
        // 1. Deduct from HQ
        PreparedStatement deduct = conn.prepareStatement(
                "UPDATE drinks SET stock = stock - ? WHERE name = ? AND branch_id = " +
                        "(SELECT id FROM branches WHERE name = 'NAIROBI')");
        deduct.setInt(1, quantity);
        deduct.setString(2, drinkName);
        deduct.executeUpdate();

        // 2. Add to branch
        PreparedStatement add = conn.prepareStatement(
                "UPDATE drinks SET stock = stock + ? WHERE name = ? AND branch_id = " +
                        "(SELECT id FROM branches WHERE name = ?)");
        add.setInt(1, quantity);
        add.setString(2, drinkName);
        add.setString(3, branchName);
        add.executeUpdate();

        // 3. Log redistribution
        logToFile("STOCK REDISTRIBUTED", quantity + " units of " + drinkName +
                " from HQ to " + branchName);
    }

    private static void logToFile(String type, String message) {
        try {
            String logEntry = String.format("[%s] %s: %s\n",
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()),
                    type, message);

            Files.write(Paths.get("server_log.txt"),
                    logEntry.getBytes(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("Failed to write to log: " + e.getMessage());
        }
    }

    private static void logError(String context, Exception e) {
        String errorMsg = String.format("[%s] ERROR in %s: %s\nStack Trace: %s\n",
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()),
                context, e.getMessage(), Arrays.toString(e.getStackTrace()));

        try {
            Files.write(Paths.get("error_log.txt"),
                    errorMsg.getBytes(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ioException) {
            System.err.println("Failed to log error: " + ioException.getMessage());
        }
    }
}