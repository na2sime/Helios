# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Helios is a revolutionary multi-database ORM supporting PostgreSQL, MongoDB, MariaDB, and **Hybrid Entities**. It features annotation-based entity mapping, relationship management, and the groundbreaking ability to distribute entity data across both relational and document databases for optimal performance and flexibility.

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