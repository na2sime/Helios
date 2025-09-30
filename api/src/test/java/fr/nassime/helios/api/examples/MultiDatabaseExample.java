package fr.nassime.helios.api.examples;

import fr.nassime.helios.api.Helios;
import fr.nassime.helios.api.HeliosSession;
import fr.nassime.helios.api.annotations.Column;
import fr.nassime.helios.api.annotations.Field;
import fr.nassime.helios.api.annotations.Id;
import fr.nassime.helios.api.annotations.Persistable;
import fr.nassime.helios.api.annotations.enums.GenerationType;
import fr.nassime.helios.api.annotations.GeneratedValue;
import fr.nassime.helios.api.annotations.enums.PersistenceType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Example demonstrating multi-database configuration and usage.
 * Shows how to connect to PostgreSQL, MongoDB, and MariaDB simultaneously
 * and route entities to the appropriate database.
 */
public class MultiDatabaseExample {

    /**
     * User entity stored in PostgreSQL "main" database
     */
    @Persistable(name = "users", type = PersistenceType.SQL, database = "main")
    public static class User {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(name = "username")
        private String username;

        @Column(name = "email")
        private String email;

        @Column(name = "created_at")
        private LocalDateTime createdAt;

        public User() {}

        public User(String username, String email) {
            this.username = username;
            this.email = email;
            this.createdAt = LocalDateTime.now();
        }

        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }

    /**
     * Analytics event stored in MongoDB "analytics" database
     */
    @Persistable(name = "events", type = PersistenceType.DOCUMENT, database = "analytics")
    public static class AnalyticsEvent {

        @Id
        private String id;

        @Field(name = "event_type")
        private String eventType;

        @Field(name = "user_id")
        private Long userId;

        @Field(name = "metadata")
        private Object metadata;

        @Field(name = "timestamp")
        private LocalDateTime timestamp;

        public AnalyticsEvent() {}

        public AnalyticsEvent(String eventType, Long userId) {
            this.eventType = eventType;
            this.userId = userId;
            this.timestamp = LocalDateTime.now();
        }

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getEventType() { return eventType; }
        public void setEventType(String eventType) { this.eventType = eventType; }

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }

        public Object getMetadata() { return metadata; }
        public void setMetadata(Object metadata) { this.metadata = metadata; }

        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }

    /**
     * Product entity stored in MariaDB "legacy" database
     */
    @Persistable(name = "products", type = PersistenceType.SQL, database = "legacy")
    public static class Product {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(name = "name")
        private String name;

        @Column(name = "price")
        private Double price;

        @Column(name = "stock")
        private Integer stock;

        public Product() {}

        public Product(String name, Double price, Integer stock) {
            this.name = name;
            this.price = price;
            this.stock = stock;
        }

        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public Double getPrice() { return price; }
        public void setPrice(Double price) { this.price = price; }

        public Integer getStock() { return stock; }
        public void setStock(Integer stock) { this.stock = stock; }
    }

    /**
     * Order entity using default database (first configured)
     */
    @Persistable(name = "orders", type = PersistenceType.SQL)
    public static class Order {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(name = "user_id")
        private Long userId;

        @Column(name = "total_amount")
        private Double totalAmount;

        @Column(name = "created_at")
        private LocalDateTime createdAt;

        public Order() {}

        public Order(Long userId, Double totalAmount) {
            this.userId = userId;
            this.totalAmount = totalAmount;
            this.createdAt = LocalDateTime.now();
        }

        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }

        public Double getTotalAmount() { return totalAmount; }
        public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }

        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }

    public static void main(String[] args) {
        // Configure Helios with multiple databases
        Helios helios = Helios.configure()
                .postgres("main", "localhost:5432/myapp", "user", "password")
                .mongo("analytics", "mongodb://localhost:27017/analytics")
                .mariadb("legacy", "localhost:3306/legacy_db", "root", "password")
                .defaultDatabase("main")  // Orders will go here by default
                .build();

        // Open a single session that works across all databases
        try (HeliosSession session = helios.openSession()) {

            // Save user to PostgreSQL "main" database
            User user = new User("john_doe", "john@example.com");
            user = session.save(user);
            System.out.println("User saved to PostgreSQL: " + user.getId());

            // Save analytics event to MongoDB "analytics" database
            AnalyticsEvent event = new AnalyticsEvent("user_registered", user.getId());
            event = session.save(event);
            System.out.println("Event saved to MongoDB: " + event.getId());

            // Save product to MariaDB "legacy" database
            Product product = new Product("Laptop", 999.99, 50);
            product = session.save(product);
            System.out.println("Product saved to MariaDB: " + product.getId());

            // Save order to default database (PostgreSQL "main")
            Order order = new Order(user.getId(), 999.99);
            order = session.save(order);
            System.out.println("Order saved to default database: " + order.getId());

            // Query across databases transparently
            List<User> users = session.findAll(User.class);  // Queries PostgreSQL
            System.out.println("Found " + users.size() + " users in PostgreSQL");

            List<AnalyticsEvent> events = session.findAll(AnalyticsEvent.class);  // Queries MongoDB
            System.out.println("Found " + events.size() + " events in MongoDB");

            List<Product> products = session.findAll(Product.class);  // Queries MariaDB
            System.out.println("Found " + products.size() + " products in MariaDB");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            helios.close();
        }
    }

    /**
     * Alternative: Use specific database sessions for fine-grained control
     */
    public static void specificDatabaseExample() {
        Helios helios = Helios.configure()
                .postgres("main", "localhost:5432/myapp", "user", "password")
                .mongo("analytics", "mongodb://localhost:27017/analytics")
                .build();

        // Open session for specific database only
        try (HeliosSession pgSession = helios.openSession("main")) {
            User user = new User("jane_doe", "jane@example.com");
            user = pgSession.save(user);
            System.out.println("User saved: " + user.getId());
        }

        // Open different session for MongoDB
        try (HeliosSession mongoSession = helios.openSession("analytics")) {
            AnalyticsEvent event = new AnalyticsEvent("page_view", 123L);
            event = mongoSession.save(event);
            System.out.println("Event saved: " + event.getId());
        }

        helios.close();
    }
}