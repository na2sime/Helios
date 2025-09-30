# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Helios is a modern multi-database ORM supporting PostgreSQL, MariaDB, and MongoDB. It features annotation-based entity mapping, relationship management, and a clean abstraction layer for seamless multi-database operations.

## Architecture

### Multi-Module Structure
- **api module** (`/api/`): Core API interfaces, annotations, and contracts
- **sql module** (`/sql/`): Common SQL functionality and abstractions
- **postgres module** (`/postgres/`): PostgreSQL-specific implementations
- **mariadb module** (`/mariadb/`): MariaDB-specific implementations
- **mongo module** (`/mongo/`): MongoDB-specific implementations

### Core Components
- `HeliosSession`: Main session interface providing CRUD operations
- `HeliosSessionFactory`: Factory for creating database sessions
- `EntityMapper`: Handles entity-to-database mapping using reflection and annotations
- `AbstractSqlSession`: Base class for all SQL implementations with connection management
- `TransactionManager`: Handles database transactions
- `RelationLoader`: Manages loading of entity relationships (OneToMany, ManyToOne, ManyToMany)
- Query builders: SQL and MongoDB query abstractions

### Annotation System
Uses unified annotation system for multi-database support:

#### Core Annotations
- `@Persistable`: Universal entity annotation for all storage types
  - `type = PersistenceType.SQL` for PostgreSQL/MariaDB
  - `type = PersistenceType.DOCUMENT` for MongoDB
- `@Id`: Marks primary key fields
- `@GeneratedValue`: Configures ID generation strategy
- `@Column`: Maps fields to relational database columns
- `@Field`: Maps fields to document database fields

#### Relationship Annotations
- `@OneToMany`, `@ManyToOne`, `@ManyToMany`, `@OneToOne`: Define entity relationships
- `@JoinColumn`: Configures foreign key columns
- `@JoinTable`: Configures join tables for many-to-many relationships

#### Multi-Database Configuration
- `database` parameter: Route entities to specific databases in multi-database setups
  - Example: `@Persistable(name = "users", database = "main-pg")`

## Development Commands

### Build Commands
```bash
# Build all modules
./gradlew build

# Build specific module
./gradlew :api:build
./gradlew :postgres:build
./gradlew :mongo:build

# Create JAR with dependencies
./gradlew shadowJar
```

### Testing
```bash
# Run all tests
./gradlew test

# Run tests for specific module
./gradlew :api:test
./gradlew :postgres:test
./gradlew :mongo:test

# Test with JUnit Platform
./gradlew test --info
```

### Clean and Rebuild
```bash
# Clean all modules
./gradlew clean

# Clean and rebuild
./gradlew clean build
```

## Key Implementation Details

### Entity Mapping
Entities are mapped using annotations and reflection via `EntityMapper`. The mapper caches entity metadata including table names, column mappings, and relationship definitions.

### Session Management
- SQL sessions extend `AbstractSqlSession` which provides common functionality
- MongoDB has its own `MongoSession` implementation
- All sessions implement the `HeliosSession` interface
- Connection pooling via HikariCP for SQL databases

### Transaction Management
- Transactions managed via `beginTransaction()` on sessions
- Support for automatic transaction management via `executeInTransaction()`
- Automatic rollback on exceptions
- SQL transactions use database connections; MongoDB uses client sessions

### Relationship Loading
- **Eager loading**: Automatically loads related entities marked as EAGER
- **Lazy loading**: Relations loaded on-demand via `loadRelation(entity, fieldName)`
- **Cascade operations**: Supports cascading saves and deletes for related entities
- **Cross-database**: Relations work within the same database type (SQL-to-SQL, Mongo-to-Mongo)

### Connection Pooling
Uses HikariCP for high-performance connection pooling in SQL databases. Configuration handled through provider-specific configuration classes.

## Multi-Database Configuration (v2.0+)

Helios v2.0 introduces simplified multi-database configuration, allowing you to connect to PostgreSQL, MariaDB, and MongoDB simultaneously with automatic entity routing.

### Configuration with HeliosBuilder
```java
// Configure multiple databases
Helios helios = Helios.configure()
    .postgres("main", "localhost:5432/mydb", "user", "password")
    .mongo("analytics", "mongodb://localhost:27017/analytics")
    .mariadb("legacy", "localhost:3306/olddb", "user", "password")
    .defaultDatabase("main")
    .build();

// Open unified session
try (HeliosSession session = helios.openSession()) {
    // Operations automatically routed to correct database
    session.save(user);      // → PostgreSQL (main)
    session.save(event);     // → MongoDB (analytics)
    session.save(product);   // → MariaDB (legacy)
}

// Or open database-specific session
try (HeliosSession pgSession = helios.openSession("main")) {
    // All operations use PostgreSQL only
    pgSession.executeInTransaction(s -> {
        s.save(user);
        s.save(order);
    });
}
```

### Entity Routing with @Persistable
```java
// User stored in PostgreSQL
@Persistable(name = "users", type = PersistenceType.SQL, database = "main")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email")
    private String email;

    @Column(name = "first_name")
    private String firstName;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Order> orders;
}

// Events stored in MongoDB
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
}

// Products stored in MariaDB
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

### Single Database Configuration (Legacy Support)
```java
// Still supported for simple use cases
HeliosSessionFactory factory = Helios.createSessionFactory(config);
HeliosSession session = factory.openSession();

// CRUD operations
User user = new User();
user.setEmail("user@example.com");
session.save(user);

Optional<User> found = session.findById(User.class, 1L);
List<User> all = session.findAll(User.class);

session.close();
```

### Query API
```java
// Create query
Query<User> query = session.createQuery(User.class)
    .where("email", QueryOperator.EQUALS, "test@example.com")
    .orderBy("firstName", SortDirection.ASC)
    .limit(10);

List<User> results = query.execute();
```

## Database Schema Requirements

### SQL Databases
Entities expect standard relational database schemas with:
- Primary key columns (typically `id`)
- Foreign key columns for relationships (e.g., `user_id`)
- Join tables for Many-to-Many relationships
- Proper column naming matching entity field names or `@Column(name="...")` annotations

### MongoDB
- Collections are created automatically
- No schema enforcement by default
- Embedded documents and arrays supported natively
- ID field can be auto-generated or manually assigned

## Dependencies

Key runtime dependencies:
- HikariCP 5.0.1 (connection pooling)
- PostgreSQL JDBC 42.7.2
- MariaDB JDBC (compatible version)
- MongoDB Java Driver (latest)
- SLF4J 2.0.9 (logging)
- Lombok 1.18.28 (code generation)

Test dependencies:
- JUnit Jupiter 5.10.0
- Mockito 5.6.0
- Testcontainers (for integration tests)

## Best Practices

1. **Always close sessions**: Use try-with-resources or explicit close()
2. **Use transactions**: Wrap multiple operations in transactions for consistency
3. **Entity design**: Keep entities simple, use proper annotations
4. **Connection pooling**: Configure HikariCP settings for production use
5. **Error handling**: Catch and handle `HeliosException` appropriately
6. **Multi-database naming**: Use descriptive database names (e.g., "user-data", "analytics")
7. **Database routing**: Group related entities in the same database
8. **Transaction scope**: Transactions don't span multiple databases - use database-specific sessions

## Multi-Database Limitations

### Cross-Database Transactions
Transactions cannot span multiple databases. Operations on entities in different databases are not atomic:

```java
// ❌ Won't work atomically - entities in different databases
session.executeInTransaction(s -> {
    s.save(user);      // PostgreSQL
    s.save(event);     // MongoDB - different database!
});

// ✅ Use database-specific sessions for transactions
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

// ✅ Keep related entities in the same database
@Persistable(database = "main")
class User {
    @OneToMany
    List<Order> orders;  // Both User and Order in "main"
}
```

## Roadmap

### v2.1 (Planned)
- Hybrid entities (data spanning SQL + NoSQL)
- Cross-storage relations
- Advanced caching layer
- Query result streaming

### v2.2 (Future)
- Reactive/async support
- Schema migration tools
- Advanced monitoring and metrics