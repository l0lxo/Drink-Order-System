import java.io.Serializable;
import java.util.Random;

public class Payment implements Serializable {
    private final double amount;
    private String status;
    private final String method;
    private String receipt;
    private static final Random random = new Random();

    public Payment(double amount, String method) {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive");
        if (!method.matches("M-Pesa|Card|Cash")) throw new IllegalArgumentException("Invalid payment method");

        this.amount = amount;
        this.method = method;
        this.status = "Pending";
        this.receipt = "N/A";
    }

    public void process() {
        this.status = "Completed"; // Force success
        this.receipt = generateReceipt(); // Generate valid receipt
    }

    private String generateReceipt() {
        return "RCPT-" + System.currentTimeMillis() + "-" + method.substring(0, 3).toUpperCase();
    }

    // Getters
    public double getAmount() { return amount; }
    public String getStatus() { return status; }
    public String getMethod() { return method; }
    public String getReceipt() { return receipt; }
}