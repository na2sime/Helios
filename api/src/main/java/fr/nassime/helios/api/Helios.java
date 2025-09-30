package fr.nassime.helios.api;

import fr.nassime.helios.api.config.DatabaseConfig;
import fr.nassime.helios.api.config.HeliosConfiguration;
import fr.nassime.helios.api.config.MultiDatabaseConfig;
import fr.nassime.helios.api.exception.ConfigurationException;
import fr.nassime.helios.api.exception.HeliosException;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * Main entry point for Helios ORM.
 * Supports both single and multi-database configurations.
 *
 * Single database usage:
 * <pre>
 * HeliosSessionFactory factory = Helios.createSessionFactory(config);
 * </pre>
 *
 * Multi-database usage:
 * <pre>
 * Helios helios = Helios.configure()
 *     .postgres("main", "localhost:5432/mydb", "user", "pass")
 *     .mongo("analytics", "mongodb://localhost:27017/analytics")
 *     .build();
 *
 * HeliosSession session = helios.openSession();
 * </pre>
 */
@Slf4j
public final class Helios {

    private final MultiDatabaseConfig config;
    private final Map<String, HeliosSessionFactory> sessionFactories = new HashMap<>();

    /**
     * Internal constructor for multi-database setup
     */
    Helios(MultiDatabaseConfig config) {
        this.config = config;
        initializeSessionFactories();
    }

    /**
     * Start building a multi-database Helios configuration
     *
     * @return A builder for configuring multiple databases
     */
    public static HeliosBuilder configure() {
        return new HeliosBuilder();
    }

    /**
     * Create a session factory using the provided configuration (single database).
     * The appropriate provider will be automatically selected based on the configuration.
     */
    public static HeliosSessionFactory createSessionFactory(HeliosConfiguration configuration) {
        log.info("Creating Helios session factory for provider: {}", configuration.getProvider());

        HeliosProvider provider = findProvider(configuration);
        provider.validateConfiguration(configuration);

        return provider.createSessionFactory(configuration);
    }

    /**
     * Open a new multi-database session.
     * This session can handle entities across all configured databases.
     */
    public HeliosSession openSession() {
        return new MultiDatabaseSession(sessionFactories, config);
    }

    /**
     * Open a session for a specific database
     */
    public HeliosSession openSession(String databaseName) {
        HeliosSessionFactory factory = sessionFactories.get(databaseName);
        if (factory == null) {
            throw new HeliosException("Database '" + databaseName + "' not configured");
        }
        return factory.openSession();
    }

    /**
     * Close all session factories and release resources
     */
    public void close() {
        for (HeliosSessionFactory factory : sessionFactories.values()) {
            try {
                if (factory instanceof AutoCloseable) {
                    ((AutoCloseable) factory).close();
                }
            } catch (Exception e) {
                log.error("Error closing session factory", e);
            }
        }
        sessionFactories.clear();
    }

    /**
     * Initialize session factories for all configured databases
     */
    private void initializeSessionFactories() {
        for (Map.Entry<String, DatabaseConfig> entry : config.getDatabases().entrySet()) {
            String name = entry.getKey();
            DatabaseConfig dbConfig = entry.getValue();

            log.info("Initializing database '{}' ({})", name, dbConfig.getType());

            try {
                HeliosSessionFactory factory = createSessionFactoryForConfig(dbConfig);
                sessionFactories.put(name, factory);
            } catch (Exception e) {
                throw new ConfigurationException("Failed to initialize database '" + name + "': " + e.getMessage(), e);
            }
        }
    }

    /**
     * Create a session factory for a specific database configuration
     */
    private HeliosSessionFactory createSessionFactoryForConfig(DatabaseConfig dbConfig) {
        HeliosConfiguration heliosConfig = fr.nassime.helios.api.config.DatabaseConfigAdapter.toHeliosConfiguration(dbConfig);
        return createSessionFactory(heliosConfig);
    }

    /**
     * Find the appropriate provider for the given configuration.
     */
    private static HeliosProvider findProvider(HeliosConfiguration configuration) {
        ServiceLoader<HeliosProvider> providers = ServiceLoader.load(HeliosProvider.class);

        for (HeliosProvider provider : providers) {
            if (provider.supports(configuration)) {
                log.debug("Found provider: {} v{}", provider.getName(), provider.getVersion());
                return provider;
            }
        }

        throw new ConfigurationException("No provider found for database type: " + configuration.getProvider());
    }
}