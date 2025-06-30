// BranchClient.java
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class BranchClient {
    private static final String SERVER = "localhost";
    private static final int PORT = 5000;
    private static final String DB_URL = "jdbc:mysql://localhost:3306/drink_order_system";
    private static final String USER = "root";
    private static final String PASS = "grace";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(BranchClient::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Customer Order Form");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 500);

        JTabbedPane tabbedPane = new JTabbedPane();

        // Order Tab
        JPanel orderPanel = createOrderPanel();
        tabbedPane.addTab("Place Order", orderPanel);

        // History Tab
        JPanel historyPanel = createHistoryPanel();
        tabbedPane.addTab("Order History", historyPanel);

        frame.add(tabbedPane);
        frame.setVisible(true);
    }

    private static JPanel createOrderPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 2, 8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel nameLabel = new JLabel("Customer Name:");
        JTextField nameField = new JTextField();

        JLabel drinkLabel = new JLabel("Drink Name:");
        JComboBox<String> drinkBox = new JComboBox<>(getDrinkList().toArray(new String[0]));

        JLabel quantityLabel = new JLabel("Quantity:");
        JTextField quantityField = new JTextField();

        JLabel branchLabel = new JLabel("Branch:");
        JComboBox<String> branchBox = new JComboBox<>(new String[]{"NAIROBI", "NAKURU", "MOMBASA", "KISUMU"});

        JLabel methodLabel = new JLabel("Payment Method:");
        JComboBox<String> methodBox = new JComboBox<>(new String[]{"M-Pesa", "Card", "Cash"});

        JTextArea outputArea = new JTextArea(8, 40);
        outputArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(outputArea);

        panel.add(nameLabel); panel.add(nameField);
        panel.add(drinkLabel); panel.add(drinkBox);
        panel.add(quantityLabel); panel.add(quantityField);
        panel.add(branchLabel); panel.add(branchBox);
        panel.add(methodLabel); panel.add(methodBox);

        JButton submitButton = new JButton("Place Order");
        submitButton.addActionListener(e -> {
            try {
                String name = nameField.getText();
                if (name.isEmpty()) {
                    outputArea.setText("Please enter customer name");
                    return;
                }

                String selected = drinkBox.getSelectedItem().toString();
                String drink = selected.split(" - ")[0];
                double price = DatabaseService.getDrinkPrice(drink);

                int orderQty = Integer.parseInt(quantityField.getText());
                if (orderQty <= 0) {
                    outputArea.setText("Quantity must be positive");
                    return;
                }

                String branch = branchBox.getSelectedItem().toString();
                String method = methodBox.getSelectedItem().toString();

                Drink drinkObj = new Drink(drink, "DefaultBrand", price, 50);
                Customer customer = new Customer(name);
                Order order = new Order(customer, drinkObj, orderQty, branch, method);

                try (Socket socket = new Socket(SERVER, PORT);
                     ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                     ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

                    out.writeObject(order);
                    String response = (String) in.readObject();
                    outputArea.setText(response);

                    // Clear fields after successful order
                    nameField.setText("");
                    quantityField.setText("");
                    branchBox.setSelectedIndex(0);
                    methodBox.setSelectedIndex(0);

                } catch (IOException | ClassNotFoundException ex) {
                    // Show full stack trace instead of just "null"
                    StringWriter sw = new StringWriter();
                    ex.printStackTrace(new PrintWriter(sw));
                    outputArea.setText("Connection error:\n" + sw.toString());
                }

            } catch (NumberFormatException ex) {
                outputArea.setText("Please enter a valid quantity.");
            }
        });


        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> {
            nameField.setText("");
            quantityField.setText("");
            branchBox.setSelectedIndex(0);
            methodBox.setSelectedIndex(0);
            outputArea.setText("");
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(submitButton);
        buttonPanel.add(clearButton);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(panel, BorderLayout.NORTH);
        mainPanel.add(buttonPanel, BorderLayout.CENTER);
        mainPanel.add(scrollPane, BorderLayout.SOUTH);

        return mainPanel;
    }

    private static JPanel createHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JTextArea historyArea = new JTextArea(15, 50);
        historyArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(historyArea);

        JLabel nameLabel = new JLabel("Customer Name:");
        JTextField nameField = new JTextField(20);
        JButton viewButton = new JButton("View History");

        viewButton.addActionListener(e -> {
            String name = nameField.getText();
            if (!name.isEmpty()) {
                String history = getCustomerHistory(name);
                historyArea.setText(history);
            }
        });

        JPanel inputPanel = new JPanel();
        inputPanel.add(nameLabel);
        inputPanel.add(nameField);
        inputPanel.add(viewButton);

        panel.add(inputPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private static List<String> getDrinkList() {
        List<String> drinks = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name, price FROM drinks ORDER BY name ASC")) {
            while (rs.next()) {
                String name = rs.getString("name");
                double price = rs.getDouble("price");
                drinks.add(name + " - KES " + price);
            }
        } catch (SQLException e) {
            drinks.add("No drinks found");
            e.printStackTrace();
        }
        return drinks;
    }

    private static String getCustomerHistory(String customerName) {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            String sql = "SELECT o.order_date, d.name AS drink, o.quantity, " +
                    "d.price, b.name AS branch " +
                    "FROM orders o JOIN drinks d ON o.drink_id = d.id " +
                    "JOIN branches b ON o.branch_id = b.id " +
                    "JOIN customers c ON o.customer_id = c.id " +
                    "WHERE c.name = ? ORDER BY o.order_date DESC";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, customerName);
            ResultSet rs = ps.executeQuery();

            StringBuilder sb = new StringBuilder("Order History for " + customerName + ":\n\n");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            double totalSpent = 0;

            while (rs.next()) {
                java.util.Date orderDate = rs.getTimestamp("order_date");
                String drink = rs.getString("drink");
                int quantity = rs.getInt("quantity");
                double price = rs.getDouble("price");
                String branch = rs.getString("branch");
                double total = quantity * price;
                totalSpent += total;

                sb.append(String.format("%s - %d x %s @ KES %,.2f (Total: KES %,.2f) - %s\n",
                        sdf.format(orderDate), quantity, drink, price, total, branch));
            }

            sb.append(String.format("\nTOTAL SPENT: KES %,.2f", totalSpent));
            return sb.toString();
        } catch (SQLException e) {
            return "Error retrieving history: " + e.getMessage();
        }
    }
}