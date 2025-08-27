package fr.nassime.helios.mongo;

import fr.nassime.helios.api.HeliosProvider;
import fr.nassime.helios.api.HeliosSessionFactory;
import fr.nassime.helios.api.config.HeliosConfiguration;
import fr.nassime.helios.api.exception.HeliosException;
import fr.nassime.helios.mongo.config.MongoConfiguration;
import fr.nassime.helios.mongo.session.MongoSessionFactory;
import lombok.extern.slf4j.Slf4j;

/**
 * MongoDB implementation of HeliosProvider.
 * Provides MongoDB-specific session factories and configurations.
 */
@Slf4j
public class MongoProvider implements HeliosProvider {
    
    private static final String PROVIDER_NAME = "mongodb";
    
    @Override
    public String getName() {
        return PROVIDER_NAME;
    }
    
    @Override
    public HeliosSessionFactory createSessionFactory(HeliosConfiguration configuration) {
        log.info("Creating MongoDB session factory");
        
        try {
            MongoConfiguration mongoConfig = extractMongoConfiguration(configuration);
            return new MongoSessionFactory(mongoConfig);
        } catch (Exception e) {
            log.error("Failed to create MongoDB session factory", e);
            throw new HeliosException("Failed to create MongoDB session factory: " + e.getMessage(), e);
        }
    }
    
    /**
     * Extract MongoDB-specific configuration from general Helios configuration.
     */
    private MongoConfiguration extractMongoConfiguration(HeliosConfiguration configuration) {
        String connectionString = configuration.getProperty("mongo.connectionString");
        if (connectionString == null || connectionString.trim().isEmpty()) {
            throw new HeliosException("MongoDB connection string is required (mongo.connectionString)");
        }
        
        String database = configuration.getProperty("mongo.database");
        if (database == null || database.trim().isEmpty()) {
            throw new HeliosException("MongoDB database name is required (mongo.database)");
        }
        
        MongoConfiguration.MongoConfigurationBuilder builder = MongoConfiguration.builder()
            .connectionString(connectionString.trim())
            .database(database.trim());
        
        // Optional connection settings
        String connectionTimeout = configuration.getProperty("mongo.connectionTimeoutMs");
        if (connectionTimeout != null && !connectionTimeout.trim().isEmpty()) {
            builder.connectionTimeoutMs(Integer.parseInt(connectionTimeout.trim()));
        }
        
        String socketTimeout = configuration.getProperty("mongo.socketTimeoutMs");
        if (socketTimeout != null && !socketTimeout.trim().isEmpty()) {
            builder.socketTimeoutMs(Integer.parseInt(socketTimeout.trim()));
        }
        
        String serverSelectionTimeout = configuration.getProperty("mongo.serverSelectionTimeoutMs");
        if (serverSelectionTimeout != null && !serverSelectionTimeout.trim().isEmpty()) {
            builder.serverSelectionTimeoutMs(Integer.parseInt(serverSelectionTimeout.trim()));
        }
        
        String maxPoolSize = configuration.getProperty("mongo.maxConnectionPoolSize");
        if (maxPoolSize != null && !maxPoolSize.trim().isEmpty()) {
            builder.maxConnectionPoolSize(Integer.parseInt(maxPoolSize.trim()));
        }
        
        String minPoolSize = configuration.getProperty("mongo.minConnectionPoolSize");
        if (minPoolSize != null && !minPoolSize.trim().isEmpty()) {
            builder.minConnectionPoolSize(Integer.parseInt(minPoolSize.trim()));
        }
        
        return builder.build();
    }
}