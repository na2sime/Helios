package fr.nassime.helios.mariadb.config;

import fr.nassime.helios.api.config.ConnectionPoolConfig;
import fr.nassime.helios.api.config.HeliosConfiguration;
import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * MariaDB-specific configuration implementation.
 */
@Data
@Builder
public class MariaDBConfiguration implements HeliosConfiguration {
    
    private static final String PROVIDER_NAME = "mariadb";
    
    private final String host;
    private final int port;
    private final String database;
    private final String schema;
    private final String username;
    private final String password;
    private final boolean ssl;
    private final ConnectionPoolConfig connectionPoolConfig;
    private final Properties additionalProperties;
    
    @Builder.Default
    private final String provider = PROVIDER_NAME;
    
    @Override
    public String getProvider() {
        return provider;
    }
    
    @Override
    public String getConnectionUrl() {
        StringBuilder url = new StringBuilder();
        url.append("jdbc:mariadb://")
           .append(host)
           .append(":")
           .append(port)
           .append("/")
           .append(database);
        
        if (ssl) {
            url.append("?useSSL=true");
        }
        
        return url.toString();
    }
    
    @Override
    public String getUsername() {
        return username;
    }
    
    @Override
    public String getPassword() {
        return password;
    }
    
    @Override
    public String getDatabase() {
        return database;
    }
    
    @Override
    public String getSchema() {
        return schema != null ? schema : database;
    }
    
    @Override
    public ConnectionPoolConfig getConnectionPoolConfig() {
        return connectionPoolConfig != null ? connectionPoolConfig : ConnectionPoolConfig.builder().build();
    }
    
    @Override
    public Properties getProperties() {
        return additionalProperties != null ? additionalProperties : new Properties();
    }
    
    @Override
    public String getProperty(String key) {
        return getProperties().getProperty(key);
    }
    
    @Override
    public String getProperty(String key, String defaultValue) {
        return getProperties().getProperty(key, defaultValue);
    }
    
    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> config = new HashMap<>();
        config.put("provider", provider);
        config.put("host", host);
        config.put("port", port);
        config.put("database", database);
        config.put("schema", getSchema());
        config.put("username", username);
        config.put("ssl", ssl);
        config.put("connectionUrl", getConnectionUrl());
        return config;
    }
    
    /**
     * Create a configuration builder with default values.
     */
    public static MariaDBConfigurationBuilder defaultConfig() {
        return new MariaDBConfigurationBuilder()
                .host("localhost")
                .port(3306)
                .ssl(false);
    }
}