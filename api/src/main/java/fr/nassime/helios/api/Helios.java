package fr.nassime.helios.api;

import fr.nassime.helios.api.config.HeliosConfiguration;
import fr.nassime.helios.api.exception.ConfigurationException;
import lombok.extern.slf4j.Slf4j;

import java.util.ServiceLoader;

/**
 * Main entry point for Helios ORM.
 * Provides a unified API to create session factories regardless of the underlying database provider.
 */
@Slf4j
public final class Helios {
    
    private Helios() {
        // Utility class
    }
    
    /**
     * Create a session factory using the provided configuration.
     * The appropriate provider will be automatically selected based on the configuration.
     */
    public static HeliosSessionFactory createSessionFactory(HeliosConfiguration configuration) {
        log.info("Creating Helios session factory for provider: {}", configuration.getProvider());
        
        HeliosProvider provider = findProvider(configuration);
        provider.validateConfiguration(configuration);
        
        return provider.createSessionFactory(configuration);
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