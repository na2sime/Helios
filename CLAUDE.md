# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Helios is a revolutionary multi-database ORM supporting PostgreSQL, MongoDB, MariaDB, **Hybrid Entities**, and **Cross-Storage Relations**. It features annotation-based entity mapping, relationship management, and the groundbreaking ability to distribute entity data AND relationships across both relational and document databases for optimal performance and flexibility.

## Architecture

### Multi-Module Structure
- **api module** (`/api/`): Core API interfaces, annotations, and contracts
- **sql module** (`/sql/`): Common SQL functionality and abstractions
- **postgres module** (`/postgres/`): PostgreSQL-specific implementations
- **mariadb module** (`/mariadb/`): MariaDB-specific implementations
- **mongo module** (`/mongo/`): MongoDB-specific implementations
- **hybrid module** (`/hybrid/`): **Revolutionary Hybrid Entity support**

### Core Components
- `HeliosORM`: Main entry point providing CRUD operations and transaction management
- `EntityMapper`: Handles entity-to-database mapping using reflection and annotations
- `ConnectionManager`: Manages database connections via HikariCP
- `TransactionManager`: Handles database transactions
- `RelationLoader`: Manages loading of entity relationships (OneToMany, ManyToOne, ManyToMany)
- Query builders: `SelectBuilder`, `InsertBuilder`, `UpdateBuilder`, `DeleteBuilder`

### Annotation System
Uses unified annotation system for multi-database support:

#### Core Annotations
- `@Persistable`: Universal entity annotation for all storage types
- `@Id`: Marks primary key fields
- `@Column`: Maps fields to relational database columns
- `@Field`: Maps fields to document database fields

#### Relationship Annotations
- `@OneToMany`, `@ManyToOne`, `@ManyToMany`, `@OneToOne`: Define entity relationships
- `@JoinColumn`, `@JoinTable`: Configure relationship mappings

#### Hybrid Entity Annotations (NEW! 🚀)
- `@HybridEntity`: Marks entities that span multiple storage types
- `@HybridField`: Controls field-level storage distribution
- `StorageType`: RELATIONAL, DOCUMENT, or BOTH
- `SynchronizationStrategy`: LINKED, EMBEDDED, UNIFIED, or MANUAL

#### Cross-Storage Relations (REVOLUTIONARY! 🌟)
- `@CrossStorageRelation`: Enables relations between SQL and NoSQL entities
- `@HybridRelation`: Advanced relation configuration for hybrid environments
- `CrossStorageStrategy`: REFERENCE, EMBED, DUPLICATE, ADAPTIVE, MATERIALIZED_VIEW
- **World's first ORM** supporting relations across different database types

## Development Commands

### Build Commands
```bash
# Build all modules
./gradlew build

# Build specific module
./gradlew :api:build
./gradlew :postgres:build  
./gradlew :mongo:build
./gradlew :hybrid:build

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

### Transaction Management
All database operations are wrapped in transactions via `TransactionManager.executeInTransaction()`. The ORM supports both automatic and manual transaction management.

### Relationship Loading
- **Eager loading**: Automatically loads related entities marked as EAGER
- **Lazy loading**: Relations loaded on-demand via `loadRelation(entity, fieldName)`
- **Cascade operations**: Supports cascading saves and deletes for related entities

### Connection Pooling
Uses HikariCP for high-performance connection pooling. Configuration handled through `DataSourceConfig` builder pattern.

### Hybrid Entities (Revolutionary Feature! 🚀)
Helios introduces **Hybrid Entities** - the first ORM to natively support distributing entity data across multiple storage types within a single entity definition.

#### Key Benefits
- **Optimal Storage**: Store structured data in SQL, flexible data in NoSQL
- **Performance**: Fast relational queries + flexible document aggregations  
- **Scalability**: Scale different data types independently
- **Evolution**: Add flexible fields without schema migrations

#### Usage Example
```java
@Persistable(name = "users", type = PersistenceType.HYBRID)
@HybridEntity(
    relationalTable = "users",
    documentCollection = "user_profiles", 
    strategy = SynchronizationStrategy.LINKED
)
public class User {
    @Id
    private Long id;
    
    // Stored in PostgreSQL for fast queries
    @HybridField(storage = StorageType.RELATIONAL)
    private String email;
    
    @HybridField(storage = StorageType.RELATIONAL) 
    private String firstName;
    
    // Stored in MongoDB for flexibility
    @HybridField(storage = StorageType.DOCUMENT)
    private List<Message> messages;
    
    @HybridField(storage = StorageType.DOCUMENT)
    private Map<String, Object> preferences;
}
```

#### Synchronization Strategies
- **LINKED**: Document references relational ID (recommended)
- **EMBEDDED**: Both stores share embedded identifiers
- **UNIFIED**: Full synchronization across both stores  
- **MANUAL**: Developer-controlled synchronization

#### Use Cases
- **E-commerce**: Product catalog (SQL) + Reviews/Analytics (NoSQL)
- **Social Media**: User profile (SQL) + Activity feed (NoSQL)
- **IoT**: Device config (SQL) + Telemetry data (NoSQL)
- **CRM**: Customer data (SQL) + Interaction logs (NoSQL)

### Cross-Storage Relations (World First! 🌟)
Helios introduces **Cross-Storage Relations** - the revolutionary ability to define relationships between entities stored in completely different database types.

#### Revolutionary Capabilities
- **SQL ↔ NoSQL Relations**: Direct relationships between PostgreSQL and MongoDB entities
- **Intelligent Synchronization**: Automatic data consistency across storage types
- **Performance Optimization**: Smart caching and materialized views
- **Zero Migration**: Add cross-storage relations without schema changes

#### Advanced Relation Strategies

**REFERENCE Strategy** (Minimal overhead)
```java
@Entity // PostgreSQL
public class User {
    @OneToMany
    @CrossStorageRelation(
        targetStorage = StorageType.DOCUMENT,
        strategy = CrossStorageStrategy.REFERENCE,
        foreignKey = "user_id"
    )
    private List<Message> messages; // Messages in MongoDB
}
```

**EMBED Strategy** (Maximum performance)
```java
@HybridEntity
public class Order {
    @OneToMany
    @CrossStorageRelation(strategy = CrossStorageStrategy.EMBED)
    private List<OrderItem> items; // Full objects embedded
}
```

**ADAPTIVE Strategy** (Intelligent optimization)
```java
@ManyToOne
@CrossStorageRelation(
    strategy = CrossStorageStrategy.ADAPTIVE,
    // Auto-embed if < 10 items, reference if more
)
private List<RecentActivity> activities;
```

**MATERIALIZED_VIEW Strategy** (Analytics optimization)
```java
@OneToMany
@CrossStorageRelation(strategy = CrossStorageStrategy.MATERIALIZED_VIEW)
@HybridRelation(
    partitionBy = "created_date",
    materialized = true,
    queryHint = "optimize_for_analytics"
)
private List<SalesMetric> metrics; // Optimized for reporting
```

#### Game-Changing Use Cases

**Multi-Database E-commerce**
```java
// Products in PostgreSQL for ACID compliance
// Reviews in MongoDB for flexible schema
// Orders span both for optimal performance

@Persistable(type = PersistenceType.SQL)
public class Product {
    @OneToMany
    @CrossStorageRelation(targetStorage = StorageType.DOCUMENT)
    private List<Review> reviews; // Reviews in MongoDB!
}

@Persistable(type = PersistenceType.HYBRID)
@HybridEntity(...)
public class Order {
    @ManyToOne
    @CrossStorageRelation(targetStorage = StorageType.RELATIONAL)
    private Product product; // Product in PostgreSQL
    
    @OneToMany
    @CrossStorageRelation(targetStorage = StorageType.DOCUMENT)
    private List<StatusEvent> events; // Events in MongoDB
}
```

**Social Platform with Hybrid Performance**
```java
@HybridEntity(...)
public class User {
    // Profile data in SQL for complex queries
    @HybridField(storage = StorageType.RELATIONAL)
    private String email, firstName, lastName;
    
    // Posts in MongoDB for flexibility and scale
    @OneToMany
    @CrossStorageRelation(
        targetStorage = StorageType.DOCUMENT,
        strategy = CrossStorageStrategy.REFERENCE
    )
    private List<Post> posts;
    
    // Friends in SQL for fast graph queries
    @ManyToMany
    @CrossStorageRelation(targetStorage = StorageType.RELATIONAL)
    private List<User> friends;
    
    // Activity in MongoDB with partitioning
    @OneToMany
    @CrossStorageRelation(targetStorage = StorageType.DOCUMENT)
    @HybridRelation(partitionBy = "timestamp", batchSize = 50)
    private List<UserActivity> recentActivity;
}
```

#### Benefits of Cross-Storage Relations
- **Best of Both Worlds**: ACID compliance + Schema flexibility
- **Performance Optimization**: Store data where it performs best
- **Horizontal Scaling**: Scale SQL and NoSQL independently
- **Future-Proof**: Add new storage types without code changes
- **Cost Optimization**: Use cheaper storage for appropriate data types

## Database Schema Requirements

Entities expect standard relational database schemas with:
- Primary key columns (typically `id`)
- Foreign key columns for relationships (e.g., `department_id`)
- Join tables for Many-to-Many relationships
- Proper column naming matching entity field names or `@Column(name="...")` annotations

## Dependencies

Key runtime dependencies:
- HikariCP 5.0.1 (connection pooling)
- PostgreSQL JDBC 42.7.2
- SLF4J 2.0.9 (logging)
- Reflections 0.10.2 (annotation scanning)
- Lombok 1.18.28 (code generation)

Test dependencies:
- JUnit Jupiter 5.10.0
- Mockito 5.6.0