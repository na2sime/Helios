package fr.nassime.helios.api;

import fr.nassime.helios.api.config.HeliosConfiguration;

/**
 * Factory interface for creating Helios sessions.
 * Each database provider should implement this interface.
 */
public interface HeliosSessionFactory extends AutoCloseable {
    
    /**
     * Create a new session.
     */
    HeliosSession openSession();
    
    /**
     * Get the current session if available.
     */
    HeliosSession getCurrentSession();
    
    /**
     * Get the configuration used by this factory.
     */
    HeliosConfiguration getConfiguration();
    
    /**
     * Check if the factory is closed.
     */
    boolean isClosed();
    
    @Override
    void close();
}