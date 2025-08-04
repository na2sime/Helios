package fr.nassime.helios.mariadb;

import fr.nassime.helios.api.HeliosProvider;
import fr.nassime.helios.mariadb.config.MariaDBConfiguration;
import fr.nassime.helios.mariadb.MariaDBProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MariaDBProvider.
 */
class MariaDBProviderTest {
    
    @Test
    void testProviderInfo() {
        HeliosProvider provider = new MariaDBProvider();
        
        assertEquals("mariadb", provider.getName());
        assertNotNull(provider.getVersion());
        assertTrue(provider.getVersion().length() > 0);
    }
    
    @Test
    void testValidConfigurationValidation() {
        MariaDBProvider provider = new MariaDBProvider();
        
        MariaDBConfiguration validConfig = MariaDBConfiguration.builder()
                .host("localhost")
                .port(3306)
                .database("test_db")
                .username("test_user")
                .password("test_pass")
                .build();
        
        assertDoesNotThrow(() -> provider.validateConfiguration(validConfig));
    }
    
    @Test
    void testInvalidConfigurationValidation() {
        MariaDBProvider provider = new MariaDBProvider();
        
        // Test null configuration
        assertThrows(RuntimeException.class, 
                () -> provider.validateConfiguration(null));
    }
    
    @Test
    void testSupports() {
        MariaDBProvider provider = new MariaDBProvider();
        
        MariaDBConfiguration mariadbConfig = MariaDBConfiguration.builder()
                .host("localhost")
                .port(3306)
                .database("test_db")
                .username("test_user")
                .password("test_pass")
                .build();
        
        assertTrue(provider.supports(mariadbConfig));
    }
}