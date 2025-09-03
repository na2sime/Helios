package fr.nassime.helios.api.config;

import fr.nassime.helios.api.annotations.enums.PersistenceType;
import fr.nassime.helios.api.id.IdConverter;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Unified configuration builder for Helios ORM supporting all storage backends.
 * This replaces the provider-specific configuration classes with a single, 
 * flexible configuration system.
 */
public class UnifiedHeliosConfiguration {
    
    private final String provider;
    private final Map<String, Object> properties;
    private final ConnectionPoolConfig poolConfig;
    private final Map<Class<?>, IdConverter<?, ?>> idConverters;
    
    private UnifiedHeliosConfiguration(Builder builder) {
        this.provider = builder.provider;
        this.properties = new HashMap<>(builder.properties);
        this.poolConfig = builder.poolConfig;
        this.idConverters = new HashMap<>(builder.idConverters);
    }
    
    /**
     * Create a new configuration builder.
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Create a configuration builder for a specific provider.
     */
    public static Builder builder(String provider) {
        return new Builder().provider(provider);
    }
    
    /**
     * Create a configuration builder with automatic provider detection.
     */
    public static Builder autoDetect() {
        return new Builder().provider("auto");
    }
    
    // Getters
    
    public String getProvider() {
        return provider;
    }
    
    public Map<String, Object> getProperties() {
        return properties;
    }
    
    public ConnectionPoolConfig getPoolConfig() {
        return poolConfig;
    }
    
    public Map<Class<?>, IdConverter<?, ?>> getIdConverters() {
        return idConverters;
    }
    
    public Object getProperty(String key) {
        return properties.get(key);
    }
    
    public String getStringProperty(String key) {
        Object value = properties.get(key);
        return value != null ? value.toString() : null;
    }
    
    public Integer getIntProperty(String key) {
        Object value = properties.get(key);
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof String) return Integer.valueOf((String) value);
        return null;
    }
    
    public Boolean getBooleanProperty(String key) {
        Object value = properties.get(key);
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof String) return Boolean.valueOf((String) value);
        return null;
    }
    
    @SuppressWarnings("unchecked")
    public <T> IdConverter<T, ?> getIdConverter(Class<T> type) {
        return (IdConverter<T, ?>) idConverters.get(type);
    }
    
    /**
     * Configuration builder with fluent interface.
     */
    public static class Builder {
        private String provider;
        private final Map<String, Object> properties = new HashMap<>();
        private ConnectionPoolConfig poolConfig = ConnectionPoolConfig.builder().build();
        private final Map<Class<?>, IdConverter<?, ?>> idConverters = new HashMap<>();
        
        private Builder() {}
        
        /**
         * Set the storage provider.
         * 
         * @param provider provider name ("postgresql", "mongodb", "mariadb", "auto")
         */
        public Builder provider(String provider) {
            this.provider = provider;
            return this;
        }
        
        /**
         * PostgreSQL configuration.
         */
        public PostgreSQLBuilder postgresql() {
            return new PostgreSQLBuilder(this);
        }
        
        /**
         * MongoDB configuration.
         */
        public MongoDBBuilder mongodb() {
            return new MongoDBBuilder(this);
        }
        
        /**
         * MariaDB configuration.
         */
        public MariaDBBuilder mariadb() {
            return new MariaDBBuilder(this);
        }
        
        /**
         * Set connection string/URL.
         */
        public Builder connectionUrl(String url) {
            properties.put("connectionUrl", url);
            return this;
        }
        
        /**
         * Set host and port.
         */
        public Builder host(String host, int port) {
            properties.put("host", host);
            properties.put("port", port);
            return this;
        }
        
        /**
         * Set database name.
         */
        public Builder database(String database) {
            properties.put("database", database);
            return this;
        }
        
        /**
         * Set credentials.
         */
        public Builder credentials(String username, String password) {
            properties.put("username", username);
            properties.put("password", password);
            return this;
        }
        
        /**
         * Set connection pooling configuration.
         */
        public Builder pooling(ConnectionPoolConfigBuilder poolBuilder) {
            this.poolConfig = poolBuilder.build();
            return this;
        }
        
        /**
         * Set a custom property.
         */
        public Builder property(String key, Object value) {
            properties.put(key, value);
            return this;
        }
        
        /**
         * Set multiple properties from a Properties object.
         */
        public Builder properties(Properties props) {
            props.forEach((key, value) -> properties.put(key.toString(), value));
            return this;
        }
        
        /**
         * Set multiple properties from a Map.
         */
        public Builder properties(Map<String, ?> props) {
            properties.putAll(props);
            return this;
        }
        
        /**
         * Register an ID converter for a specific type.
         */
        public <T> Builder idConverter(Class<T> type, IdConverter<T, ?> converter) {
            idConverters.put(type, converter);
            return this;
        }
        
        /**
         * Build the configuration.
         */
        public UnifiedHeliosConfiguration build() {
            if (provider == null) {
                throw new IllegalStateException("Provider must be specified");
            }
            
            return new UnifiedHeliosConfiguration(this);
        }
    }
    
    /**
     * PostgreSQL-specific builder.
     */
    public static class PostgreSQLBuilder {
        private final Builder parent;
        
        private PostgreSQLBuilder(Builder parent) {
            this.parent = parent.provider("postgresql");
        }
        
        public PostgreSQLBuilder host(String host) {
            return host(host, 5432);
        }
        
        public PostgreSQLBuilder host(String host, int port) {
            parent.host(host, port);
            return this;
        }
        
        public PostgreSQLBuilder database(String database) {
            parent.database(database);
            return this;
        }
        
        public PostgreSQLBuilder credentials(String username, String password) {
            parent.credentials(username, password);
            return this;
        }
        
        public PostgreSQLBuilder schema(String schema) {
            parent.property("schema", schema);
            return this;
        }
        
        public PostgreSQLBuilder ssl(boolean enabled) {
            parent.property("ssl", enabled);
            return this;
        }
        
        public Builder and() {
            return parent;
        }
        
        public UnifiedHeliosConfiguration build() {
            return parent.build();
        }
    }
    
    /**
     * MongoDB-specific builder.
     */
    public static class MongoDBBuilder {
        private final Builder parent;
        
        private MongoDBBuilder(Builder parent) {
            this.parent = parent.provider("mongodb");
        }
        
        public MongoDBBuilder connectionString(String connectionString) {
            parent.connectionUrl(connectionString);
            return this;
        }
        
        public MongoDBBuilder host(String host) {
            return host(host, 27017);
        }
        
        public MongoDBBuilder host(String host, int port) {
            parent.host(host, port);
            return this;
        }
        
        public MongoDBBuilder database(String database) {
            parent.database(database);
            return this;
        }
        
        public MongoDBBuilder credentials(String username, String password) {
            parent.credentials(username, password);
            return this;
        }
        
        public MongoDBBuilder authDatabase(String authDatabase) {
            parent.property("authDatabase", authDatabase);
            return this;
        }
        
        public MongoDBBuilder replicaSet(String replicaSet) {
            parent.property("replicaSet", replicaSet);
            return this;
        }
        
        public Builder and() {
            return parent;
        }
        
        public UnifiedHeliosConfiguration build() {
            return parent.build();
        }
    }
    
    /**
     * MariaDB-specific builder.
     */
    public static class MariaDBBuilder {
        private final Builder parent;
        
        private MariaDBBuilder(Builder parent) {
            this.parent = parent.provider("mariadb");
        }
        
        public MariaDBBuilder host(String host) {
            return host(host, 3306);
        }
        
        public MariaDBBuilder host(String host, int port) {
            parent.host(host, port);
            return this;
        }
        
        public MariaDBBuilder database(String database) {
            parent.database(database);
            return this;
        }
        
        public MariaDBBuilder credentials(String username, String password) {
            parent.credentials(username, password);
            return this;
        }
        
        public MariaDBBuilder charset(String charset) {
            parent.property("charset", charset);
            return this;
        }
        
        public MariaDBBuilder ssl(boolean enabled) {
            parent.property("ssl", enabled);
            return this;
        }
        
        public Builder and() {
            return parent;
        }
        
        public UnifiedHeliosConfiguration build() {
            return parent.build();
        }
    }
    
    /**
     * Connection pool configuration builder.
     */
    public static class ConnectionPoolConfigBuilder {
        private int maxSize = 10;
        private int minSize = 2;
        private long maxLifetime = 1800000; // 30 minutes
        private long idleTimeout = 600000;  // 10 minutes
        private long connectionTimeout = 30000; // 30 seconds
        
        public ConnectionPoolConfigBuilder maxSize(int maxSize) {
            this.maxSize = maxSize;
            return this;
        }
        
        public ConnectionPoolConfigBuilder minSize(int minSize) {
            this.minSize = minSize;
            return this;
        }
        
        public ConnectionPoolConfigBuilder maxLifetime(long maxLifetime) {
            this.maxLifetime = maxLifetime;
            return this;
        }
        
        public ConnectionPoolConfigBuilder idleTimeout(long idleTimeout) {
            this.idleTimeout = idleTimeout;
            return this;
        }
        
        public ConnectionPoolConfigBuilder connectionTimeout(long connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
            return this;
        }
        
        ConnectionPoolConfig build() {
            return ConnectionPoolConfig.builder()
                    .maximumPoolSize(maxSize)
                    .minimumIdle(minSize)
                    .maxLifetime(maxLifetime)
                    .idleTimeout(idleTimeout)
                    .connectionTimeout(connectionTimeout)
                    .build();
        }
    }
    
    /**
     * Factory method to create a pool configuration builder.
     */
    public static ConnectionPoolConfigBuilder poolConfig() {
        return new ConnectionPoolConfigBuilder();
    }
}