# Multi-Database Configuration Guide

## Overview

Helios v2.0 supports connecting to multiple databases simultaneously. You can configure PostgreSQL, MariaDB, and MongoDB in a single application and route entities to the appropriate database using simple annotations.

## Quick Start

### 1. Configure Multiple Databases

```java
Helios helios = Helios.configure()
    .postgres("main-pg", "localhost:5432/mydb", "user", "password")
    .mongo("analytics", "mongodb://localhost:27017/analytics")
    .mariadb("legacy", "localhost:3306/olddb", "user", "password")
    .defaultDatabase("main-pg")
    .build();
```

### 2. Define Entities with Database Routing

```java
// User stored in PostgreSQL
@Persistable(name = "users", type = PersistenceType.SQL, database = "main-pg")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username")
    private String username;

    @Column(name = "email")
    private String email;
}

// Analytics stored in MongoDB
@Persistable(name = "events", type = PersistenceType.DOCUMENT, database = "analytics")
public class AnalyticsEvent {
    @Id
    private String id;

    @Field(name = "event_type")
    private String eventType;

    @Field(name = "user_id")
    private Long userId;
}

// Product stored in MariaDB
@Persistable(name = "products", type = PersistenceType.SQL, database = "legacy")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "price")
    private Double price;
}
```

### 3. Use Single Session for All Databases

```java
try (HeliosSession session = helios.openSession()) {

    // Save to PostgreSQL
    User user = new User("john", "john@example.com");
    user = session.save(user);

    // Save to MongoDB
    AnalyticsEvent event = new AnalyticsEvent("user_created", user.getId());
    event = session.save(event);

    // Save to MariaDB
    Product product = new Product("Laptop", 999.99);
    product = session.save(product);

    // Query from appropriate databases automatically
    List<User> users = session.findAll(User.class);  // Queries PostgreSQL
    List<AnalyticsEvent> events = session.findAll(AnalyticsEvent.class);  // Queries MongoDB
    List<Product> products = session.findAll(Product.class);  // Queries MariaDB
}
```

## Configuration Options

### PostgreSQL

```java
.postgres(String name, String url, String username, String password)
.postgres(String name, String url, String username, String password, int poolSize)
```

**URL formats:**
- Simple: `"localhost:5432/mydb"`
- JDBC: `"jdbc:postgresql://localhost:5432/mydb"`

### MariaDB

```java
.mariadb(String name, String url, String username, String password)
.mariadb(String name, String url, String username, String password, int poolSize)
```

**URL formats:**
- Simple: `"localhost:3306/mydb"`
- JDBC: `"jdbc:mariadb://localhost:3306/mydb"`

### MongoDB

```java
.mongo(String name, String connectionString)
.mongo(String name, String connectionString, String username, String password)
```

**Connection string formats:**
- Simple: `"localhost:27017/mydb"`
- Full: `"mongodb://localhost:27017/mydb"`
- With options: `"mongodb://localhost:27017/mydb?replicaSet=rs0"`

## Advanced Usage

### Use Specific Database Session

For fine-grained control or transactions, open a session for a specific database:

```java
// PostgreSQL session only
try (HeliosSession pgSession = helios.openSession("main-pg")) {
    pgSession.executeInTransaction(session -> {
        User user = new User("jane", "jane@example.com");
        user = session.save(user);

        Order order = new Order(user.getId(), 100.0);
        order = session.save(order);
    });
}

// MongoDB session only
try (HeliosSession mongoSession = helios.openSession("analytics")) {
    List<AnalyticsEvent> todayEvents = mongoSession
        .createQuery(AnalyticsEvent.class)
        .where("timestamp", QueryOperator.GREATER_THAN, LocalDateTime.now().minusDays(1))
        .execute();
}
```

### Default Database

Entities without explicit `database` parameter use the default database:

```java
Helios helios = Helios.configure()
    .postgres("main", "localhost:5432/mydb", "user", "pass")
    .mongo("analytics", "mongodb://localhost:27017/analytics")
    .defaultDatabase("main")  // Orders will go here
    .build();

// Uses default database (main)
@Persistable(name = "orders", type = PersistenceType.SQL)
public class Order {
    // ...
}
```

### Connection Pool Configuration

```java
.postgres("main", "localhost:5432/mydb", "user", "pass", 20)  // Pool size: 20
.mariadb("legacy", "localhost:3306/old", "user", "pass", 5)   // Pool size: 5
```

## Best Practices

### 1. Use Descriptive Database Names

```java
.postgres("user-data", ...)      // ✅ Clear purpose
.mongo("events", ...)            // ✅ Clear purpose
.mariadb("legacy-orders", ...)   // ✅ Clear purpose

.postgres("db1", ...)            // ❌ Unclear
```

### 2. Group Related Entities

```java
// User-related entities → PostgreSQL "main"
@Persistable(database = "main") class User { }
@Persistable(database = "main") class UserProfile { }
@Persistable(database = "main") class Order { }

// Analytics entities → MongoDB "analytics"
@Persistable(database = "analytics") class PageView { }
@Persistable(database = "analytics") class ClickEvent { }
```

### 3. Use Type Parameter for Clarity

```java
@Persistable(name = "users", type = PersistenceType.SQL, database = "main")
@Persistable(name = "events", type = PersistenceType.DOCUMENT, database = "analytics")
```

### 4. Close Helios Instance

```java
Helios helios = Helios.configure()...build();
try {
    // Use helios
} finally {
    helios.close();  // Closes all connections
}
```

## Limitations

### Multi-Database Transactions

Transactions across multiple databases are not supported. Use database-specific sessions:

```java
// ❌ Won't work - entities in different databases
session.executeInTransaction(s -> {
    s.save(user);      // PostgreSQL
    s.save(event);     // MongoDB - different database!
});

// ✅ Use specific database sessions
helios.openSession("main").executeInTransaction(s -> {
    s.save(user);
    s.save(order);  // Both in same database
});
```

### Cross-Database Relations

Relations between entities in different databases are not supported:

```java
// ❌ User in PostgreSQL, Orders in MongoDB
@Persistable(database = "main")
class User {
    @OneToMany
    List<Order> orders;  // Won't work if Order is in different database
}
```

## Migration from Single Database

### Before (v1.x)

```java
PostgreSQLConfiguration config = new PostgreSQLConfiguration();
config.setHost("localhost");
config.setPort(5432);
config.setDatabase("mydb");
config.setUsername("user");
config.setPassword("password");

HeliosSessionFactory factory = Helios.createSessionFactory(config);
HeliosSession session = factory.openSession();
```

### After (v2.0)

```java
Helios helios = Helios.configure()
    .postgres("main", "localhost:5432/mydb", "user", "password")
    .build();

HeliosSession session = helios.openSession();
```

### Entities Stay the Same

```java
// No changes needed to entity annotations
@Persistable(name = "users", type = PersistenceType.SQL)
public class User {
    @Id
    private Long id;
    // ...
}
```

Just add `database = "name"` when using multiple databases:

```java
@Persistable(name = "users", type = PersistenceType.SQL, database = "main")
public class User {
    @Id
    private Long id;
    // ...
}
```

## Complete Example

See `MultiDatabaseExample.java` in `api/src/test/java/fr/nassime/helios/api/examples/` for a complete working example.