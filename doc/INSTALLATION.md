# Helios Installation Guide

Complete guide for installing and using Helios ORM in your project.

---

## Quick Start

### Option 1: Use helios-all (Recommended)

The simplest way to get started with Helios is to use the all-in-one JAR that includes all database providers.

#### Gradle

```groovy
dependencies {
    implementation 'fr.nassime.helios:helios-all:2.0.0'
}
```

#### Maven

```xml
<dependency>
    <groupId>fr.nassime.helios</groupId>
    <artifactId>helios-all</artifactId>
    <version>2.0.0</version>
</dependency>
```

### Option 2: Modular Installation

If you only need specific database support, you can include only the modules you need:

#### Gradle

```groovy
dependencies {
    // Core API (always required)
    implementation 'fr.nassime.helios:helios-api:2.0.0'

    // Add only the providers you need
    implementation 'fr.nassime.helios:helios-postgres:2.0.0'   // PostgreSQL
    implementation 'fr.nassime.helios:helios-mariadb:2.0.0'    // MariaDB
    implementation 'fr.nassime.helios:helios-mongo:2.0.0'      // MongoDB
}
```

#### Maven

```xml
<dependencies>
    <!-- Core API (always required) -->
    <dependency>
        <groupId>fr.nassime.helios</groupId>
        <artifactId>helios-api</artifactId>
        <version>2.0.0</version>
    </dependency>

    <!-- PostgreSQL support -->
    <dependency>
        <groupId>fr.nassime.helios</groupId>
        <artifactId>helios-postgres</artifactId>
        <version>2.0.0</version>
    </dependency>

    <!-- MariaDB support -->
    <dependency>
        <groupId>fr.nassime.helios</groupId>
        <artifactId>helios-mariadb</artifactId>
        <version>2.0.0</version>
    </dependency>

    <!-- MongoDB support -->
    <dependency>
        <groupId>fr.nassime.helios</groupId>
        <artifactId>helios-mongo</artifactId>
        <version>2.0.0</version>
    </dependency>
</dependencies>
```

---

## Installation from GitHub Packages

To use Helios from GitHub Packages, you need to configure authentication.

### Configure GitHub Packages

#### Gradle

Add to your `build.gradle`:

```groovy
repositories {
    mavenCentral()
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/na2sime/helios")
        credentials {
            username = project.findProperty("gpr.user") ?: System.getenv("GITHUB_ACTOR")
            password = project.findProperty("gpr.key") ?: System.getenv("GITHUB_TOKEN")
        }
    }
}
```

Add to `~/.gradle/gradle.properties`:

```properties
gpr.user=YOUR_GITHUB_USERNAME
gpr.key=YOUR_GITHUB_TOKEN
```

#### Maven

Add to your `~/.m2/settings.xml`:

```xml
<settings>
  <servers>
    <server>
      <id>github</id>
      <username>YOUR_GITHUB_USERNAME</username>
      <password>YOUR_GITHUB_TOKEN</password>
    </server>
  </servers>
</settings>
```

Add to your `pom.xml`:

```xml
<repositories>
  <repository>
    <id>github</id>
    <url>https://maven.pkg.github.com/na2sime/helios</url>
  </repository>
</repositories>
```

---

## Manual Installation (Development)

For development or if you want to build from source:

### Clone the Repository

```bash
git clone https://github.com/na2sime/helios.git
cd helios
```

### Build All Modules

```bash
./gradlew build
```

### Build Fat JAR Only

```bash
./gradlew :helios-all:shadowJar
```

The JAR will be available at: `helios-all/build/libs/helios-2.0.0-SNAPSHOT-all.jar`

### Install to Local Maven Repository

```bash
./gradlew publishToMavenLocal
```

Then use in your project:

```groovy
dependencies {
    implementation 'fr.nassime.helios:helios-all:2.0.0-SNAPSHOT'
}

repositories {
    mavenLocal()
    mavenCentral()
}
```

---

## Requirements

- **Java 17** or higher
- **Gradle 7.0+** or **Maven 3.6+**

### Database Requirements (Optional)

Install only the databases you need:

- **PostgreSQL 12+** (for PostgreSQL support)
- **MariaDB 10.6+** (for MariaDB support)
- **MongoDB 4.4+** (for MongoDB support)

---

## Verify Installation

Create a simple test to verify Helios is working:

```java
import fr.nassime.helios.api.Helios;
import fr.nassime.helios.api.HeliosSession;

public class HeliosTest {
    public static void main(String[] args) {
        Helios helios = Helios.configure()
            .postgres("test", "localhost:5432/testdb", "user", "pass")
            .build();

        try (HeliosSession session = helios.openSession()) {
            System.out.println("Helios initialized successfully!");
        }

        helios.close();
    }
}
```

---

## Module Comparison

| Module | Size | Use Case |
|--------|------|----------|
| `helios-all` | ~11MB | **Recommended** - Complete package with all providers |
| `helios-api` + providers | ~8-11MB | Custom - Choose only what you need |
| Individual modules | ~2-4MB each | Minimal - Smallest footprint |

### When to Use Each Option

**Use `helios-all` if:**
- You want the simplest setup
- You're using multiple databases
- Size is not a critical concern
- You're building a fat JAR application

**Use modular installation if:**
- You only need one database type
- You're building a microservice with size constraints
- You want fine-grained control over dependencies

---

## Excluding Providers from helios-all

If you're using `helios-all` but want to exclude specific providers:

### Gradle

```groovy
dependencies {
    implementation('fr.nassime.helios:helios-all:2.0.0') {
        exclude group: 'fr.nassime.helios', module: 'helios-mongo'  // Exclude MongoDB
    }
}
```

### Maven

```xml
<dependency>
    <groupId>fr.nassime.helios</groupId>
    <artifactId>helios-all</artifactId>
    <version>2.0.0</version>
    <exclusions>
        <exclusion>
            <groupId>fr.nassime.helios</groupId>
            <artifactId>helios-mongo</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

---

## Next Steps

After installation:

1. **[Configuration Guide](CONFIGURATION.md)** - Learn all configuration options
2. **[Multi-Database Guide](MULTI_DATABASE.md)** - Set up multiple databases
3. **[Examples](../api/src/test/java/fr/nassime/helios/api/examples/)** - See working code examples

---

## Troubleshooting

### "No provider found for database type"

**Problem:** Helios can't find the database provider.

**Solution:** Make sure you've included the appropriate module:
- PostgreSQL: `helios-postgres`
- MariaDB: `helios-mariadb`
- MongoDB: `helios-mongo`

Or use `helios-all` which includes everything.

### "Version conflict" errors

**Problem:** Dependency version conflicts with other libraries.

**Solution:** Use `helios-all` which uses shaded dependencies to avoid conflicts, or explicitly set versions in your build file.

### "ClassNotFoundException" at runtime

**Problem:** Missing dependencies at runtime.

**Solution:**
- For fat JAR builds, use `helios-all`
- For modular builds, ensure all required modules are in your runtime classpath

---

## Support

- 🐛 **Issues:** [GitHub Issues](https://github.com/na2sime/helios/issues)
- 💡 **Discussions:** [GitHub Discussions](https://github.com/na2sime/helios/discussions)