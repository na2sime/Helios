package fr.nassime.helios.mariadb;

import fr.nassime.helios.api.HeliosProvider;
import fr.nassime.helios.api.HeliosSessionFactory;
import fr.nassime.helios.api.config.HeliosConfiguration;
import fr.nassime.helios.api.exception.ConfigurationException;
import fr.nassime.helios.mariadb.session.MariaDBSessionFactory;
import lombok.extern.slf4j.Slf4j;

/**
 * MariaDB implementation of HeliosProvider.
 */
@Slf4j
public class MariaDBProvider implements HeliosProvider {
    
    private static final String PROVIDER_NAME = "mariadb";
    private static final String VERSION = "2.0.0-SNAPSHOT";
    
    @Override
    public String getName() {
        return PROVIDER_NAME;
    }
    
    @Override
    public String getVersion() {
        return VERSION;
    }
    
    @Override
    public HeliosSessionFactory createSessionFactory(HeliosConfiguration configuration) {
        log.info("Creating MariaDB session factory");
        validateConfiguration(configuration);
        return new MariaDBSessionFactory(configuration);
    }
    
    @Override
    public boolean supports(HeliosConfiguration configuration) {
        return PROVIDER_NAME.equalsIgnoreCase(configuration.getProvider()) ||
               "mysql".equalsIgnoreCase(configuration.getProvider());
    }
    
    @Override
    public void validateConfiguration(HeliosConfiguration configuration) {
        if (configuration == null) {
            throw new ConfigurationException("Configuration cannot be null");
        }
        
        if (!supports(configuration)) {
            throw new ConfigurationException("Unsupported provider: " + configuration.getProvider());
        }
        
        if (isBlank(configuration.getConnectionUrl())) {
            throw new ConfigurationException("Connection URL is required");
        }
        
        if (isBlank(configuration.getUsername())) {
            throw new ConfigurationException("Username is required");
        }
        
        if (isBlank(configuration.getDatabase())) {
            throw new ConfigurationException("Database name is required");
        }
        
        // Validate connection URL format
        String url = configuration.getConnectionUrl();
        if (!url.startsWith("jdbc:mariadb://") && !url.startsWith("jdbc:mysql://")) {
            throw new ConfigurationException("Invalid MariaDB/MySQL connection URL format");
        }
        
        log.debug("MariaDB configuration validated successfully");
    }
    
    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }
}