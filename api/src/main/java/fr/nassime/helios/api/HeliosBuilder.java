package fr.nassime.helios.api;

import fr.nassime.helios.api.config.DatabaseConfig;
import fr.nassime.helios.api.config.MultiDatabaseConfig;

/**
 * Fluent builder for configuring Helios with multiple databases.
 * Supports simple and advanced configuration options.
 *
 * Simple usage:
 * <pre>
 * Helios helios = Helios.configure()
 *     .postgres("main", "localhost:5432/mydb", "user", "pass")
 *     .mongo("analytics", "mongodb://localhost:27017/analytics")
 *     .build();
 * </pre>
 *
 * Advanced usage:
 * <pre>
 * Helios helios = Helios.configure()
 *     .database("main")
 *         .postgres("localhost:5432/mydb", "user", "pass")
 *         .poolSize(20, 5)
 *         .timeout(30000, 600000, 1800000)
 *         .ssl(true)
 *         .leakDetection(true)
 *     .and()
 *     .build();
 * </pre>
 */
public class HeliosBuilder {

    private final MultiDatabaseConfig config = new MultiDatabaseConfig();

    /**
     * Add a PostgreSQL database connection (simple)
     */
    public HeliosBuilder postgres(String name, String url, String username, String password) {
        return addDatabase(name, "postgresql", url, username, password, 10);
    }

    /**
     * Add a PostgreSQL database connection with custom pool size
     */
    public HeliosBuilder postgres(String name, String url, String username, String password, int poolSize) {
        return addDatabase(name, "postgresql", url, username, password, poolSize);
    }

    /**
     * Add a MariaDB database connection (simple)
     */
    public HeliosBuilder mariadb(String name, String url, String username, String password) {
        return addDatabase(name, "mariadb", url, username, password, 10);
    }

    /**
     * Add a MariaDB database connection with custom pool size
     */
    public HeliosBuilder mariadb(String name, String url, String username, String password, int poolSize) {
        return addDatabase(name, "mariadb", url, username, password, poolSize);
    }

    /**
     * Add a MongoDB database connection (simple)
     */
    public HeliosBuilder mongo(String name, String connectionString) {
        return addDatabase(name, "mongodb", connectionString, null, null, 10);
    }

    /**
     * Add a MongoDB database connection with authentication
     */
    public HeliosBuilder mongo(String name, String connectionString, String username, String password) {
        return addDatabase(name, "mongodb", connectionString, username, password, 10);
    }

    /**
     * Start configuring a database with advanced options
     *
     * @param name Unique identifier for this database
     * @return Advanced database configuration builder
     */
    public DatabaseBuilder database(String name) {
        return new DatabaseBuilder(this, name);
    }

    /**
     * Set the default database for entities without explicit database specification
     */
    public HeliosBuilder defaultDatabase(String name) {
        config.setDefaultDatabase(name);
        return this;
    }

    /**
     * Build the Helios instance
     */
    public Helios build() {
        if (config.getDatabases().isEmpty()) {
            throw new IllegalStateException("At least one database must be configured");
        }
        return new Helios(config);
    }

    /**
     * Internal method to add a database configuration
     */
    private HeliosBuilder addDatabase(String name, String type, String url,
                                       String username, String password, int poolSize) {
        DatabaseConfig dbConfig = DatabaseConfig.builder()
                .name(name)
                .type(type)
                .url(url)
                .username(username)
                .password(password)
                .poolSize(poolSize)
                .build();

        config.addDatabase(dbConfig);
        return this;
    }

    /**
     * Advanced database configuration builder for fine-grained control.
     * Allows configuring connection pooling, timeouts, SSL, and custom properties.
     */
    public static class DatabaseBuilder {
        private final HeliosBuilder parent;
        private final DatabaseConfig.DatabaseConfigBuilder configBuilder;

        private DatabaseBuilder(HeliosBuilder parent, String name) {
            this.parent = parent;
            this.configBuilder = DatabaseConfig.builder().name(name);
        }

        /**
         * Configure as PostgreSQL database
         */
        public DatabaseBuilder postgres(String url, String username, String password) {
            configBuilder.type("postgresql").url(url).username(username).password(password);
            return this;
        }

        /**
         * Configure as MariaDB database
         */
        public DatabaseBuilder mariadb(String url, String username, String password) {
            configBuilder.type("mariadb").url(url).username(username).password(password);
            return this;
        }

        /**
         * Configure as MongoDB database
         */
        public DatabaseBuilder mongo(String connectionString) {
            configBuilder.type("mongodb").url(connectionString);
            return this;
        }

        /**
         * Configure as MongoDB database with authentication
         */
        public DatabaseBuilder mongo(String connectionString, String username, String password) {
            configBuilder.type("mongodb").url(connectionString).username(username).password(password);
            return this;
        }

        /**
         * Configure connection pool size
         *
         * @param maxPoolSize Maximum number of connections
         * @param minPoolSize Minimum number of idle connections
         */
        public DatabaseBuilder poolSize(int maxPoolSize, int minPoolSize) {
            configBuilder.poolSize(maxPoolSize).minPoolSize(minPoolSize);
            return this;
        }

        /**
         * Configure connection pool with max size only
         */
        public DatabaseBuilder poolSize(int maxPoolSize) {
            configBuilder.poolSize(maxPoolSize);
            return this;
        }

        /**
         * Configure timeout values
         *
         * @param connectionTimeout Time to wait for connection (ms)
         * @param idleTimeout How long a connection can be idle (ms)
         * @param maxLifetime Maximum lifetime of a connection (ms)
         */
        public DatabaseBuilder timeout(long connectionTimeout, long idleTimeout, long maxLifetime) {
            configBuilder.connectionTimeout(connectionTimeout)
                    .idleTimeout(idleTimeout)
                    .maxLifetime(maxLifetime);
            return this;
        }

        /**
         * Configure connection timeout only
         */
        public DatabaseBuilder connectionTimeout(long milliseconds) {
            configBuilder.connectionTimeout(milliseconds);
            return this;
        }

        /**
         * Enable or disable SSL/TLS
         */
        public DatabaseBuilder ssl(boolean enabled) {
            configBuilder.ssl(enabled);
            return this;
        }

        /**
         * Enable connection leak detection
         */
        public DatabaseBuilder leakDetection(boolean enabled) {
            configBuilder.leakDetection(enabled);
            if (enabled && configBuilder.build().getLeakDetectionThreshold() == 0) {
                configBuilder.leakDetectionThreshold(60000); // 1 minute default
            }
            return this;
        }

        /**
         * Set leak detection threshold
         */
        public DatabaseBuilder leakDetectionThreshold(long milliseconds) {
            configBuilder.leakDetection(true).leakDetectionThreshold(milliseconds);
            return this;
        }

        /**
         * Set schema name for SQL databases
         */
        public DatabaseBuilder schema(String schema) {
            configBuilder.schema(schema);
            return this;
        }

        /**
         * Add a custom property
         */
        public DatabaseBuilder property(String key, Object value) {
            configBuilder.property(key, value);
            return this;
        }

        /**
         * Finish configuring this database and return to main builder
         */
        public HeliosBuilder and() {
            parent.config.addDatabase(configBuilder.build());
            return parent;
        }

        /**
         * Finish configuring this database and build Helios
         */
        public Helios build() {
            return and().build();
        }
    }
}