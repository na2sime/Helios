package fr.nassime.helios.mongo.session;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import fr.nassime.helios.api.HeliosSession;
import fr.nassime.helios.api.HeliosSessionFactory;
import fr.nassime.helios.api.exception.HeliosException;
import fr.nassime.helios.mongo.config.MongoConfiguration;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * MongoDB implementation of HeliosSessionFactory.
 * Creates and manages MongoDB sessions using MongoDB Java Driver.
 */
@Slf4j
public class MongoSessionFactory implements HeliosSessionFactory {
    
    private final MongoConfiguration configuration;
    private final MongoClient mongoClient;
    private volatile boolean closed = false;
    
    public MongoSessionFactory(MongoConfiguration configuration) {
        this.configuration = configuration;
        this.mongoClient = createMongoClient();
        log.info("MongoDB session factory initialized for database: {}", configuration.getDatabase());
    }
    
    @Override
    public HeliosSession openSession() {
        if (closed) {
            throw new IllegalStateException("SessionFactory is closed");
        }
        log.debug("Opening MongoDB session");
        return new MongoSession(mongoClient, configuration.getDatabase());
    }
    
    @Override
    public HeliosSession getCurrentSession() {
        // MongoDB doesn't have the concept of "current session" like Hibernate
        // Each call creates a new lightweight session
        return openSession();
    }
    
    @Override
    public fr.nassime.helios.api.config.HeliosConfiguration getConfiguration() {
        // We need to adapt MongoConfiguration to HeliosConfiguration
        // For now, return null as this is used mainly for debugging
        return null;
    }
    
    @Override
    public boolean isClosed() {
        return closed;
    }
    
    @Override
    public void close() {
        log.info("Closing MongoDB session factory");
        closed = true;
        if (mongoClient != null) {
            mongoClient.close();
        }
    }
    
    /**
     * Create MongoDB client with configuration settings.
     */
    private MongoClient createMongoClient() {
        try {
            log.debug("Creating MongoDB client with connection string: {}", configuration.getConnectionString());
            
            MongoClientSettings.Builder settingsBuilder = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(configuration.getConnectionString()));
            
            // Apply timeout settings
            settingsBuilder.applyToSocketSettings(builder -> {
                if (configuration.getConnectionTimeoutMs() > 0) {
                    builder.connectTimeout(configuration.getConnectionTimeoutMs(), TimeUnit.MILLISECONDS);
                }
                if (configuration.getSocketTimeoutMs() > 0) {
                    builder.readTimeout(configuration.getSocketTimeoutMs(), TimeUnit.MILLISECONDS);
                }
            });
            
            // Apply server selection timeout
            settingsBuilder.applyToClusterSettings(builder -> {
                if (configuration.getServerSelectionTimeoutMs() > 0) {
                    builder.serverSelectionTimeout(configuration.getServerSelectionTimeoutMs(), TimeUnit.MILLISECONDS);
                }
            });
            
            // Apply connection pool settings
            settingsBuilder.applyToConnectionPoolSettings(builder -> {
                if (configuration.getMaxConnectionPoolSize() > 0) {
                    builder.maxSize(configuration.getMaxConnectionPoolSize());
                }
                if (configuration.getMinConnectionPoolSize() >= 0) {
                    builder.minSize(configuration.getMinConnectionPoolSize());
                }
                if (configuration.getMaxConnectionIdleTimeMs() > 0) {
                    builder.maxConnectionIdleTime(configuration.getMaxConnectionIdleTimeMs(), TimeUnit.MILLISECONDS);
                }
                if (configuration.getMaxConnectionLifeTimeMs() > 0) {
                    builder.maxConnectionLifeTime(configuration.getMaxConnectionLifeTimeMs(), TimeUnit.MILLISECONDS);
                }
            });
            
            return MongoClients.create(settingsBuilder.build());
            
        } catch (Exception e) {
            log.error("Failed to create MongoDB client", e);
            throw new HeliosException("Failed to create MongoDB client: " + e.getMessage(), e);
        }
    }
}