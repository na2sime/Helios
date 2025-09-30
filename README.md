# Helios ORM 🌟

**A modern, lightweight multi-database ORM for Java**

Helios is a powerful Object-Relational Mapper supporting PostgreSQL, MariaDB, and MongoDB with a unified API. Connect to multiple databases simultaneously and route entities automatically based on annotations.

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://openjdk.java.net/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Version](https://img.shields.io/badge/Version-2.0.0--SNAPSHOT-green.svg)](https://github.com/na2sime/helios)

---

## Features ✨

- 🎯 **Multi-Database Support**: PostgreSQL, MariaDB, and MongoDB
- 🔄 **Unified API**: Single interface for all database types
- 🚀 **Simple Configuration**: Connect to multiple databases with one fluent API
- 📍 **Smart Routing**: Entities automatically routed to the correct database
- 🔌 **Easy Integration**: Annotation-based entity mapping
- 🧑‍🤝‍🧑 **Relations Support**: OneToMany, ManyToOne, ManyToMany, OneToOne
- ⚡ **High Performance**: HikariCP connection pooling for SQL databases
- 🏗️ **Repository Pattern**: Optional repository abstraction

---

## Quick Start 🛠️

### Installation

#### Gradle

```groovy
dependencies {
    implementation 'fr.nassime.helios:api:2.0.0-SNAPSHOT'
    implementation 'fr.nassime.helios:postgres:2.0.0-SNAPSHOT'  // Optional
    implementation 'fr.nassime.helios:mariadb:2.0.0-SNAPSHOT'   // Optional
    implementation 'fr.nassime.helios:mongo:2.0.0-SNAPSHOT'     // Optional
}
```

#### Maven

```xml
<dependency>
    <groupId>fr.nassime.helios</groupId>
    <artifactId>api</artifactId>
    <version>2.0.0-SNAPSHOT</version>
</dependency>
<!-- Add database providers as needed -->
<dependency>
    <groupId>fr.nassime.helios</groupId>
    <artifactId>postgres</artifactId>
    <version>2.0.0-SNAPSHOT</version>
</dependency>
```

---

## Usage 📖

### Single Database Configuration

```java
// Simple PostgreSQL setup
Helios helios = Helios.configure()
    .postgres("main", "localhost:5432/mydb", "user", "password")
    .build();

try (HeliosSession session = helios.openSession()) {
    User user = new User("john", "john@example.com");
    user = session.save(user);

    Optional<User> found = session.findById(User.class, user.getId());
}
```

### Multi-Database Configuration

Connect to PostgreSQL, MongoDB, and MariaDB simultaneously:

```java
Helios helios = Helios.configure()
    .postgres("main", "localhost:5432/mydb", "user", "password")
    .mongo("analytics", "mongodb://localhost:27017/analytics")
    .mariadb("legacy", "localhost:3306/olddb", "user", "password")
    .defaultDatabase("main")
    .build();

try (HeliosSession session = helios.openSession()) {
    // Automatically routed to correct databases
    session.save(user);      // → PostgreSQL
    session.save(event);     // → MongoDB
    session.save(product);   // → MariaDB
}
```

---

## Entity Definitions 🏗️

### SQL Entity (PostgreSQL/MariaDB)

```java
@Persistable(name = "users", type = PersistenceType.SQL, database = "main")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username")
    private String username;

    @Column(name = "email")
    private String email;

    @OneToMany(mappedBy = "user")
    private List<Order> orders;

    // Getters and setters...
}
```

### MongoDB Entity

```java
@Persistable(name = "events", type = PersistenceType.DOCUMENT, database = "analytics")
public class AnalyticsEvent {

    @Id
    private String id;

    @Field(name = "event_type")
    private String eventType;

    @Field(name = "user_id")
    private Long userId;

    @Field(name = "metadata")
    private Map<String, Object> metadata;

    @Field(name = "timestamp")
    private LocalDateTime timestamp;

    // Getters and setters...
}
```

### Relationships

```java
@Persistable(name = "orders", type = PersistenceType.SQL)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToMany
    @JoinTable(
        name = "order_products",
        joinColumn = "order_id",
        inverseJoinColumn = "product_id"
    )
    private List<Product> products;
}
```

---

## CRUD Operations 🗂️

### Save

```java
User user = new User("jane", "jane@example.com");
user = session.save(user);
```

### Find by ID

```java
Optional<User> found = session.findById(User.class, 1L);
found.ifPresent(u -> System.out.println(u.getUsername()));
```

### Find All

```java
List<User> users = session.findAll(User.class);
```

### Query API

```java
List<User> activeUsers = session.createQuery(User.class)
    .where("active", QueryOperator.EQUALS, true)
    .where("age", QueryOperator.GREATER_THAN, 18)
    .orderBy("username", SortDirection.ASC)
    .limit(10)
    .execute();
```

### Delete

```java
session.delete(user);
```

### Load Relations

```java
User user = session.findById(User.class, 1L).orElseThrow();
session.loadRelation(user, "orders");  // Lazy load orders
```

---

## Transactions 💼

```java
session.executeInTransaction(s -> {
    User user = new User("bob", "bob@example.com");
    user = s.save(user);

    Order order = new Order(user.getId(), 100.0);
    s.save(order);

    // Auto-commit on success, rollback on exception
});
```

---

## Advanced Features 🚀

### Database-Specific Sessions

```java
// Open session for specific database
try (HeliosSession pgSession = helios.openSession("main")) {
    pgSession.executeInTransaction(s -> {
        // All operations use PostgreSQL
    });
}
```

### Repository Pattern

```java
public interface UserRepository extends Repository<User, Long> {
    List<User> findByEmail(String email);
}

// Implementation auto-generated by Helios
UserRepository repo = RepositoryFactory.create(UserRepository.class, session);
List<User> users = repo.findByEmail("john@example.com");
```

### Native Queries

```java
List<User> users = session.executeNativeQuery(
    "SELECT * FROM users WHERE age > ?",
    User.class,
    18
);
```

---

## Configuration Options ⚙️

### Simple Configuration

```java
// Basic connection with defaults
Helios.configure()
    .postgres("main", "localhost:5432/mydb", "user", "pass")
    .build();

// Custom pool size
Helios.configure()
    .postgres("main", "localhost:5432/mydb", "user", "pass", 20)
    .build();
```

### Advanced Configuration

```java
Helios helios = Helios.configure()
    .database("main")
        .postgres("localhost:5432/mydb", "user", "pass")
        .poolSize(50, 10)              // Max: 50, Min idle: 10
        .timeout(30000, 600000, 1800000) // Connection, Idle, Max lifetime (ms)
        .ssl(true)                      // Enable SSL/TLS
        .leakDetection(true)            // Detect connection leaks
        .schema("public")               // Set schema
        .property("cachePrepStmts", true) // Custom property
    .and()
    .build();
```

### Connection Pooling Options

| Option | Description | Default |
|--------|-------------|---------|
| `poolSize(max)` | Maximum connections | 10 |
| `poolSize(max, min)` | Max and min idle connections | 10, 2 |
| `connectionTimeout(ms)` | Wait time for connection | 30000 (30s) |
| `timeout(conn, idle, max)` | All timeout values | 30s, 10m, 30m |

### Performance & Monitoring

```java
.database("production")
    .postgres("prod-db:5432/app", "user", "pass")
    .poolSize(100, 20)                    // Large pool for high traffic
    .leakDetectionThreshold(120000)       // Alert after 2 minutes
    .property("preparedStatementCacheSize", 250)
    .property("applicationName", "MyApp")
.and()
```

### SSL/TLS Configuration

```java
.database("secure")
    .postgres("secure.example.com:5432/db", "user", "pass")
    .ssl(true)
    .property("sslmode", "require")
    .property("sslcert", "/path/to/cert.pem")
.and()
```

### URL Formats

**PostgreSQL:**
- Simple: `"localhost:5432/mydb"`
- JDBC: `"jdbc:postgresql://localhost:5432/mydb"`
- With options: `"localhost:5432/mydb?ssl=true"`

**MariaDB:**
- Simple: `"localhost:3306/mydb"`
- JDBC: `"jdbc:mariadb://localhost:3306/mydb"`

**MongoDB:**
- Simple: `"localhost:27017/mydb"`
- Full: `"mongodb://localhost:27017/mydb"`
- Cluster: `"mongodb://host1:27017,host2:27017/mydb?replicaSet=rs0"`

---

## Documentation 📚

- [Configuration Guide](doc/CONFIGURATION.md) - Complete configuration reference with all options
- [Multi-Database Guide](doc/MULTI_DATABASE.md) - Multi-database setup and best practices
- [Examples](api/src/test/java/fr/nassime/helios/api/examples/) - Full code examples

---

## Architecture 🏛️

Helios follows a modular architecture:

```
helios/
├── api/          - Core interfaces and annotations
├── sql/          - Common SQL functionality
├── postgres/     - PostgreSQL implementation
├── mariadb/      - MariaDB implementation
└── mongo/        - MongoDB implementation
```

Each database provider is independent and can be included as needed.

---

## Requirements 📋

- Java 17 or higher
- One or more of:
  - PostgreSQL 12+ (optional)
  - MariaDB 10.6+ (optional)
  - MongoDB 4.4+ (optional)

---

## Contributing 🤝

We welcome contributions! Please see our contributing guidelines.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## Roadmap 🗺️

### v2.1 (Planned)
- Hybrid entities (data spanning SQL + NoSQL)
- Cross-storage relations
- Advanced caching layer
- Query result streaming

### v2.2 (Future)
- Reactive/async support
- Schema migration tools
- Advanced monitoring and metrics

---

## License 📜

Helios is released under the [MIT License](LICENSE).

---

## Authors ✍️

- **Nassime** - [GitHub](https://github.com/na2sime)
- **Contributors** - See [CONTRIBUTORS.md](CONTRIBUTORS.md)

---

## Support 💬

- 🐛 Issues: [GitHub Issues](https://github.com/na2sime/helios/issues)
- 💡 Discussions: [GitHub Discussions](https://github.com/na2sime/helios/discussions)

---

**Made with ❤️ by NA2SIME**