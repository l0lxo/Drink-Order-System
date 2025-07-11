# Drink-Order-System
*A distributed Java application for managing drink orders across multiple branches.*

## Project Overview
This system automates operations for a drink-selling business with branches in **Nairobi (HQ), Nakuru, Mombasa, and Kisumu**. Features include:
- **Order processing** via branch clients.
- **Stock management** with low-stock alerts.
- **Sales reports** (branch revenue, customer history).
- **Real-time admin dashboard**.

## Technologies Used
- **Java** (Swing for GUI, Socket programming for networking)
- **MySQL** (Database: `drink_order_system`)
- **Maven** (Dependency management, if applicable)

## Setup Instructions
Set the database up by running the Database file in mySQL.
Run the system by:
1. Start the Server (HQ - Nairobi):
    cd src/
    javac MainServer.java
    java MainServer
2. Launch Branch Clients (On separate devices):
    javac BranchClient.java
    java BranchClient
3. Open Admin UI (On admin device):
    javac AdminUI.java
    java AdminUI
Note: Update SERVER = "localhost" in BranchClient.java for multi-device demo.

## Key Features
Place Order: Customers order drinks at branches (M-Pesa/Card/Cash).
Stock Alerts: Pop-up notifications when stock < threshold (configurable in settings table).
Sales Reports: View revenue by branch, customer history, and business summaries.
Stock Redistribution: HQ automatically redistributes stock to branches when levels are low.

## Demo Flow (Presentation)
1. Admin: Launch AdminUI, show empty reports.
2. Branch Clients (3 devices): Place orders simultaneously (e.g., 5x "Soda" in Nakuru, 3x "Juice" in Mombasa).
3. Admin:
    Demonstrate real-time alerts when stock dips below threshold.
    Show updated reports (Revenue → Branch Performance → Customer History).
    Clear alerts and restock drinks.

## Troubleshooting
"Connection refused": Ensure MainServer is running before clients start.
Database errors: Verify MySQL credentials in DB_URL, USER, PASS (all files).
Empty reports: Check if orders exist in the orders table.
To add the mysql connector for IDEs:
Eclipse/VS Code:
Right-click project → Build Path → Add External JARs → Select the connector JAR
IntelliJ:
File → Project Structure → Libraries → "+" → Java → Select the JAR


## Project Structure
src/
├── MainServer.java       # Central server (order processing)
├── BranchClient.java     # Client for branch orders
├── AdminUI.java          # Admin dashboard
├── Order.java            # Order data model
├── Payment.java          # Payment processing
└── DatabaseService.java  # DB helper methods

