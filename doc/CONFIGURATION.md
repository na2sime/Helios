# Helios Configuration Guide

Complete guide to configuring Helios ORM with all available options.

---

## Table of Contents

- [Quick Start](#quick-start)
- [Simple Configuration](#simple-configuration)
- [Advanced Configuration](#advanced-configuration)
- [Connection Pooling](#connection-pooling)
- [Timeout Configuration](#timeout-configuration)
- [SSL/TLS Security](#ssltls-security)
- [Connection Leak Detection](#connection-leak-detection)
- [Custom Properties](#custom-properties)
- [Production Best Practices](#production-best-practices)

---

## Quick Start

```java
// Minimal configuration
Helios helios = Helios.configure()
    .postgres("main", "localhost:5432/mydb", "user", "password")
    .build();

// Use it
try (HeliosSession session = helios.openSession()) {
    User user = session.findById(User.class, 1L).orElseThrow();
}
```

---

## Simple Configuration

### Single Database

```java
Helios helios = Helios.configure()
    .postgres("main", "localhost:5432/mydb", "user", "password")
    .build();
```

### Multiple Databases

```java
Helios helios = Helios.configure()
    .postgres("users", "localhost:5432/users", "user", "pass")
    .mariadb("products", "localhost:3306/products", "user", "pass")
    .mongo("analytics", "mongodb://localhost:27017/analytics")
    .defaultDatabase("users")
    .build();
```

### With Custom Pool Size

```java
Helios helios = Helios.configure()
    .postgres("main", "localhost:5432/mydb", "user", "pass", 20)  // Pool size: 20
    .build();
```

---

## Advanced Configuration

Use the `.database(name)` method for fine-grained control:

```java
Helios helios = Helios.configure()
    .database("production")
        .postgres("prod-db.example.com:5432/maindb", "app_user", "secure_pass")
        .poolSize(50, 10)                    // Max: 50, Min idle: 10
        .timeout(30000, 600000, 1800000)     // Connection, Idle, Max lifetime
        .ssl(true)
        .leakDetection(true)
        .schema("public")
        .property("applicationName", "MyApp")
        .property("cachePrepStmts", true)
    .and()
    .defaultDatabase("production")
    .build();
```

---

## Connection Pooling

### Basic Pool Configuration

```java
.database("main")
    .postgres("localhost:5432/db", "user", "pass")
    .poolSize(20)        // Max connections: 20 (min calculated automatically)
.and()
```

### Advanced Pool Configuration

```java
.database("main")
    .postgres("localhost:5432/db", "user", "pass")
    .poolSize(50, 10)    // Max: 50, Min idle: 10
.and()
```

### Pool Size Guidelines

| Scenario | Max Pool Size | Min Idle | Notes |
|----------|---------------|----------|-------|
| Development | 5 | 1 | Small pool, easy debugging |
| Small application | 10-20 | 2-5 | Default settings work well |
| Medium application | 20-50 | 5-10 | Standard production setup |
| High traffic | 50-100 | 10-20 | Monitor and adjust based on load |
| Microservice | 5-15 | 2-5 | Keep pools small, scale horizontally |

**Formula:** `Max Pool Size ≈ (Core Count × 2) + Disk Count`

---

## Timeout Configuration

### Individual Timeouts

```java
.database("main")
    .postgres("localhost:5432/db", "user", "pass")
    .connectionTimeout(30000)    // 30 seconds to get a connection
.and()
```

### All Timeouts

```java
.database("main")
    .postgres("localhost:5432/db", "user", "pass")
    .timeout(
        30000,     // Connection timeout: 30 seconds
        600000,    // Idle timeout: 10 minutes
        1800000    // Max lifetime: 30 minutes
    )
.and()
```

### Timeout Recommendations

| Timeout Type | Development | Production | High Traffic |
|--------------|-------------|------------|--------------|
| Connection | 30s | 10s | 5s |
| Idle | 10m | 10m | 5m |
| Max Lifetime | 30m | 30m | 20m |

---

## SSL/TLS Security

### Enable SSL

```java
.database("secure")
    .postgres("secure.example.com:5432/db", "user", "pass")
    .ssl(true)
.and()
```

### SSL with Certificates

```java
.database("secure")
    .postgres("secure.example.com:5432/db", "user", "pass")
    .ssl(true)
    .property("sslmode", "require")
    .property("sslcert", "/path/to/client-cert.pem")
    .property("sslkey", "/path/to/client-key.pem")
    .property("sslrootcert", "/path/to/ca-cert.pem")
.and()
```

### MongoDB SSL

```java
.database("secure-mongo")
    .mongo("mongodb://secure.example.com:27017/db?ssl=true")
    .property("sslInvalidHostNameAllowed", false)
.and()
```

---

## Connection Leak Detection

### Enable Leak Detection

```java
.database("main")
    .postgres("localhost:5432/db", "user", "pass")
    .leakDetection(true)    // Uses default threshold (60 seconds)
.and()
```

### Custom Leak Detection Threshold

```java
.database("main")
    .postgres("localhost:5432/db", "user", "pass")
    .leakDetectionThreshold(120000)    // Alert after 2 minutes
.and()
```

### Recommended Thresholds

- **Development:** 30 seconds (quick feedback)
- **Testing:** 60 seconds (default)
- **Production:** 120 seconds (avoid false positives)

---

## Custom Properties

### Database-Specific Properties

#### PostgreSQL

```java
.database("pg")
    .postgres("localhost:5432/db", "user", "pass")
    .property("preparedStatementCacheSize", 250)
    .property("cachePrepStmts", true)
    .property("applicationName", "HeliosApp")
    .property("connectTimeout", 10)
    .property("socketTimeout", 30)
    .property("loginTimeout", 5)
.and()
```

#### MariaDB

```java
.database("maria")
    .mariadb("localhost:3306/db", "user", "pass")
    .property("useServerPrepStmts", true)
    .property("cachePrepStmts", true)
    .property("prepStmtCacheSize", 250)
    .property("prepStmtCacheSqlLimit", 2048)
    .property("useLocalSessionState", true)
.and()
```

#### MongoDB

```java
.database("mongo")
    .mongo("mongodb://localhost:27017/db")
    .property("readPreference", "secondaryPreferred")
    .property("writeConcern", "majority")
    .property("maxPoolSize", 50)
    .property("minPoolSize", 10)
    .property("maxIdleTimeMS", 600000)
    .property("maxConnectionIdleTimeMS", 120000)
.and()
```

---

## Production Best Practices

### High-Traffic Production Setup

```java
Helios helios = Helios.configure()
    .database("primary")
        .postgres("prod-cluster:5432/maindb", "app_user", "secure_pass")
        .poolSize(100, 20)                       // Large pool
        .timeout(10000, 300000, 1800000)         // Aggressive timeouts
        .ssl(true)                               // Always use SSL in production
        .leakDetectionThreshold(120000)          // 2 minutes
        .property("applicationName", "MyApp")
        .property("preparedStatementCacheSize", 250)
        .property("cachePrepStmts", true)
    .and()
    .defaultDatabase("primary")
    .build();
```

### Development Setup

```java
Helios helios = Helios.configure()
    .database("dev")
        .postgres("localhost:5432/dev", "dev", "dev")
        .poolSize(5, 1)                 // Small pool
        .leakDetection(true)            // Catch issues early
        .leakDetectionThreshold(30000)  // Fast feedback
        .property("logUnclosedConnections", true)
    .and()
    .build();
```

### Read Replica Configuration

```java
Helios helios = Helios.configure()
    .database("write")
        .postgres("primary.example.com:5432/db", "writer", "pass")
        .poolSize(20, 5)
        .ssl(true)
    .and()
    .database("read")
        .postgres("replica.example.com:5432/db", "reader", "pass")
        .poolSize(50, 10)            // More connections for reads
        .property("readOnly", true)
        .ssl(true)
    .and()
    .defaultDatabase("write")
    .build();
```

### Multi-Region Setup

```java
Helios helios = Helios.configure()
    .database("us-east")
        .postgres("us-east.example.com:5432/db", "user", "pass")
        .ssl(true)
        .property("connectTimeout", 5)
    .and()
    .database("eu-west")
        .postgres("eu-west.example.com:5432/db", "user", "pass")
        .ssl(true)
        .property("connectTimeout", 5)
    .and()
    .database("analytics")
        .mongo("mongodb://analytics-cluster:27017/metrics")
        .property("readPreference", "nearest")
    .and()
    .defaultDatabase("us-east")
    .build();
```

---

## Configuration Checklist

### Development ✓
- [ ] Use small connection pools (5-10)
- [ ] Enable leak detection with short threshold (30s)
- [ ] Use localhost URLs
- [ ] Keep credentials simple
- [ ] No SSL for local databases

### Staging ✓
- [ ] Medium connection pools (10-30)
- [ ] Enable leak detection (60s threshold)
- [ ] Use staging database URLs
- [ ] Enable SSL
- [ ] Test production configuration

### Production ✓
- [ ] Appropriately sized connection pools (50-100)
- [ ] Enable leak detection (120s threshold)
- [ ] Always use SSL/TLS
- [ ] Set connection timeouts
- [ ] Configure prepared statement caching
- [ ] Set application name for monitoring
- [ ] Use secure credential management
- [ ] Configure multiple regions/replicas if needed

---

## Examples

See [AdvancedConfigurationExample.java](../api/src/test/java/fr/nassime/helios/api/examples/AdvancedConfigurationExample.java) for complete working examples of all configuration options.

---

## Need Help?

- 🐛 Issues: [GitHub Issues](https://github.com/na2sime/helios/issues)
- 💡 Discussions: [GitHub Discussions](https://github.com/na2sime/helios/discussions)