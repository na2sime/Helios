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

#### Legacy Annotations (Deprecated)
- `@Table`: Use `@Persistable(type = PersistenceType.SQL)` instead
- `@Document`: Use `@Persistable(type = PersistenceType.DOCUMENT)` instead

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

## Usage Examples

### PostgreSQL Entity
```java
@Persistable(name = "users", type = PersistenceType.SQL)
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
```

### MongoDB Document
```java
@Persistable(name = "products", type = PersistenceType.DOCUMENT)
public class Product {
    @Id
    private String id;

    @Field
    private String name;

    @Field
    private Double price;

    @Field
    private Map<String, Object> metadata;
}
```

### Session Usage
```java
// Create session
HeliosSessionFactory factory = new PostgreSQLSessionFactory(config);
HeliosSession session = factory.openSession();

// CRUD operations
User user = new User();
user.setEmail("user@example.com");
session.save(user);

Optional<User> found = session.findById(User.class, 1L);
List<User> all = session.findAll(User.class);

// Transactions
session.executeInTransaction(s -> {
    User u = s.findById(User.class, 1L).orElseThrow();
    u.setEmail("updated@example.com");
    s.save(u);
});

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

## Roadmap

### v2.1 (Future)
- Hybrid entities (data spanning SQL + NoSQL)
- Cross-storage relations
- Advanced caching layer
- Query result streaming

### v2.2 (Future)
- Reactive/async support
- Schema migration tools
- Advanced monitoring and metrics