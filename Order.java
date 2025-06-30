// Order.java
import java.io.Serializable;
import java.util.Arrays;

public class Order implements Serializable {
    private final Customer customer;
    private final Drink drink;
    private final int quantity;
    private final String branchName;
    private final Payment payment;
    private String orderStatus;

    public Order(Customer customer, Drink drink, int quantity, String branchName, String method) {
        validateInputs(customer, drink, quantity, branchName, method);

        this.customer = customer;
        this.drink = drink;
        this.quantity = quantity;
        this.branchName = branchName;
        this.payment = new Payment(getTotalPrice(), method);
        this.orderStatus = "Pending";
    }

    private void validateInputs(Customer customer, Drink drink, int quantity,
                                String branchName, String method) {
        if (customer == null) throw new IllegalArgumentException("Customer cannot be null");
        if (drink == null) throw new IllegalArgumentException("Drink cannot be null");
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive");
        if (!Arrays.asList("NAIROBI", "NAKURU", "MOMBASA", "KISUMU").contains(branchName))
            throw new IllegalArgumentException("Invalid branch name");
    }

    public void processOrder() {
        this.orderStatus = "Processing";
        payment.process();
        this.orderStatus = payment.getStatus();
    }

    public double getTotalPrice() {
        return quantity * drink.getPrice();
    }

    // Getters
    public Customer getCustomer() { return customer; }
    public Drink getDrink() { return drink; }
    public int getQuantity() { return quantity; }
    public String getBranchName() { return branchName; }
    public Payment getPayment() { return payment; }
    public String getOrderStatus() { return orderStatus; }

    @Override
    public String toString() {
        return String.format("Order[%s, %dx%s@KES%,.2f, %s, Status: %s]",
                customer.getName(), quantity, drink.getName(), drink.getPrice(),
                branchName, orderStatus);
    }
}