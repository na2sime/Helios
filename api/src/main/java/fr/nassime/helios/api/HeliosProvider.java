package fr.nassime.helios.api;

import fr.nassime.helios.api.config.HeliosConfiguration;

/**
 * Interface for database provider implementations.
 * Each database provider (PostgreSQL, MongoDB, etc.) should implement this interface.
 */
public interface HeliosProvider {
    
    /**
     * Get the name of this provider (e.g., "postgresql", "mongodb").
     */
    String getName();
    
    /**
     * Get the supported version of this provider.
     */
    String getVersion();
    
    /**
     * Create a session factory for this provider.
     */
    HeliosSessionFactory createSessionFactory(HeliosConfiguration configuration);
    
    /**
     * Check if this provider supports the given configuration.
     */
    boolean supports(HeliosConfiguration configuration);
    
    /**
     * Validate the configuration for this provider.
     */
    void validateConfiguration(HeliosConfiguration configuration);
}