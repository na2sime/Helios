# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Helios is a lightweight Object-Relational Mapper (ORM) for PostgreSQL with support for MongoDB, designed around annotation-based entity mapping and relationship management. The project uses HikariCP for connection pooling and supports lazy/eager loading of relations.

## Architecture

### Multi-Module Structure
- **Root module** (`/src/main/java/fr/nassime/helios/`): Core ORM implementation with PostgreSQL support
- **api module** (`/api/`): API layer components (placeholder for future development)  
- **postgres module** (`/postgres/`): PostgreSQL-specific implementations
- **mongo module** (`/mongo/`): MongoDB-specific implementations

### Core Components
- `HeliosORM`: Main entry point providing CRUD operations and transaction management
- `EntityMapper`: Handles entity-to-database mapping using reflection and annotations
- `ConnectionManager`: Manages database connections via HikariCP
- `TransactionManager`: Handles database transactions
- `RelationLoader`: Manages loading of entity relationships (OneToMany, ManyToOne, ManyToMany)
- Query builders: `SelectBuilder`, `InsertBuilder`, `UpdateBuilder`, `DeleteBuilder`

### Annotation System
Uses custom annotations for entity mapping:
- `@Table`: Maps classes to database tables
- `@Column`: Maps fields to database columns  
- `@Id`: Marks primary key fields
- `@OneToMany`, `@ManyToOne`, `@ManyToMany`: Define entity relationships
- `@Relation`: Base relation annotation

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

### Transaction Management
All database operations are wrapped in transactions via `TransactionManager.executeInTransaction()`. The ORM supports both automatic and manual transaction management.

### Relationship Loading
- **Eager loading**: Automatically loads related entities marked as EAGER
- **Lazy loading**: Relations loaded on-demand via `loadRelation(entity, fieldName)`
- **Cascade operations**: Supports cascading saves and deletes for related entities

### Connection Pooling
Uses HikariCP for high-performance connection pooling. Configuration handled through `DataSourceConfig` builder pattern.

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