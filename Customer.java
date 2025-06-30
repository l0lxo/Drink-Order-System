// Customer.java
public class Customer implements java.io.Serializable {
    private String name;

    public Customer(String name) {
        this.name = name;
    }

    public String getName() { return name; }
}
