// DatabaseService.java
import java.sql.*;

public class DatabaseService {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/drink_order_system";
    private static final String USER = "root";
    private static final String PASS = "grace";

    public static boolean isDrinkAvailable(String drinkName, int quantityRequested) {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT stock FROM drinks WHERE name = ? AND stock >= ?");
            ps.setString(1, drinkName);
            ps.setInt(2, quantityRequested);
            return ps.executeQuery().next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean updateDrinkStock(String drinkName, int quantityPurchased) {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            PreparedStatement ps = conn.prepareStatement(
                    "UPDATE drinks SET stock = stock - ? WHERE name = ?");
            ps.setInt(1, quantityPurchased);
            ps.setString(2, drinkName);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static double getDrinkPrice(String drinkName) {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT price FROM drinks WHERE name = ?");
            ps.setString(1, drinkName);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getDouble("price") : 0.0;
        } catch (SQLException e) {
            e.printStackTrace();
            return 0.0;
        }
    }

    public static boolean isStockLowAtBranch(String drinkName, String branchName, int threshold) {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT stock FROM drinks WHERE name = ? AND branch_id = " +
                            "(SELECT id FROM branches WHERE name = ?)");
            ps.setString(1, drinkName);
            ps.setString(2, branchName);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt("stock") < threshold;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static int getHqStock(String drinkName) {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT stock FROM drinks WHERE name = ? AND branch_id = " +
                            "(SELECT id FROM branches WHERE name = 'NAIROBI')");
            ps.setString(1, drinkName);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt("stock") : 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static boolean redistributeStock(String drinkName, String fromBranch, String toBranch, int quantity) {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(DB_URL, USER, PASS);
            conn.setAutoCommit(false);

            // 1. Deduct from source branch (HQ)
            PreparedStatement deduct = conn.prepareStatement(
                    "UPDATE drinks SET stock = stock - ? WHERE name = ? AND branch_id = " +
                            "(SELECT id FROM branches WHERE name = ?) AND stock >= ?");
            deduct.setInt(1, quantity);
            deduct.setString(2, drinkName);
            deduct.setString(3, fromBranch);
            deduct.setInt(4, quantity);

            int rowsAffected = deduct.executeUpdate();
            if (rowsAffected == 0) {
                conn.rollback();
                return false;
            }

            // 2. Check if destination branch already has this drink
            PreparedStatement check = conn.prepareStatement(
                    "SELECT id FROM drinks WHERE name = ? AND branch_id = " +
                            "(SELECT id FROM branches WHERE name = ?)");
            check.setString(1, drinkName);
            check.setString(2, toBranch);
            ResultSet rs = check.executeQuery();

            if (rs.next()) {
                // Update existing record
                PreparedStatement update = conn.prepareStatement(
                        "UPDATE drinks SET stock = stock + ? WHERE id = ?");
                update.setInt(1, quantity);
                update.setInt(2, rs.getInt("id"));
                update.executeUpdate();
            } else {
                // Insert new record (with same price as HQ)
                PreparedStatement insert = conn.prepareStatement(
                        "INSERT INTO drinks (name, brand, price, stock, branch_id) " +
                                "SELECT d.name, d.brand, d.price, ?, b.id " +
                                "FROM drinks d, branches b " +
                                "WHERE d.name = ? AND b.name = ? AND d.branch_id = " +
                                "(SELECT id FROM branches WHERE name = 'NAIROBI')");
                insert.setInt(1, quantity);
                insert.setString(2, drinkName);
                insert.setString(3, toBranch);
                insert.executeUpdate();
            }

            // 3. Log the redistribution
            PreparedStatement log = conn.prepareStatement(
                    "INSERT INTO stock_redistribution_log (drink_name, from_branch, to_branch, quantity) " +
                            "VALUES (?, ?, ?, ?)");
            log.setString(1, drinkName);
            log.setString(2, fromBranch);
            log.setString(3, toBranch);
            log.setInt(4, quantity);
            log.executeUpdate();

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }
}