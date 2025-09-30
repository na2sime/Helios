package fr.nassime.helios.postgres;

import fr.nassime.helios.api.Helios;
import fr.nassime.helios.api.HeliosSessionFactory;
import fr.nassime.helios.api.config.ConnectionPoolConfig;
import fr.nassime.helios.api.exception.ConfigurationException;
import fr.nassime.helios.postgres.config.PostgreSQLConfiguration;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for PostgreSQLProvider.
 */
class PostgreSQLProviderTest {
    
    @Test
    void shouldCreateProvider() {
        PostgreSQLProvider provider = new PostgreSQLProvider();
        
        assertThat(provider.getName()).isEqualTo("postgresql");
        assertThat(provider.getVersion()).isEqualTo("2.0.0-SNAPSHOT");
    }
    
    @Test
    void shouldSupportPostgreSQLConfiguration() {
        PostgreSQLProvider provider = new PostgreSQLProvider();
        
        PostgreSQLConfiguration config = PostgreSQLConfiguration.defaultConfig()
                .database("test_db")
                .username("test_user")
                .password("test_pass")
                .build();
        
        assertThat(provider.supports(config)).isTrue();
    }
    
    @Test
    void shouldValidateConfiguration() {
        PostgreSQLProvider provider = new PostgreSQLProvider();
        
        PostgreSQLConfiguration validConfig = PostgreSQLConfiguration.defaultConfig()
                .database("test_db")
                .username("test_user")
                .password("test_pass")
                .build();
        
        assertThatCode(() -> provider.validateConfiguration(validConfig))
                .doesNotThrowAnyException();
    }
    
    @Test
    void shouldThrowExceptionForInvalidConfiguration() {
        PostgreSQLProvider provider = new PostgreSQLProvider();
        
        PostgreSQLConfiguration invalidConfig = PostgreSQLConfiguration.defaultConfig()
                .database("") // Empty database name
                .username("test_user")
                .password("test_pass")
                .build();
        
        assertThatThrownBy(() -> provider.validateConfiguration(invalidConfig))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("Database name is required");
    }
    
    // @Test  // Disabled: requires real database connection
    void shouldCreateSessionFactoryViaHelios() {
        PostgreSQLConfiguration config = PostgreSQLConfiguration.defaultConfig()
                .host("localhost")
                .port(5432)
                .database("test_db")
                .username("test_user")
                .password("test_pass")
                .connectionPoolConfig(ConnectionPoolConfig.builder()
                        .maximumPoolSize(5)
                        .minimumIdle(1)
                        .build())
                .build();
        
        // Test provider discovery and factory creation (without connection)
        assertThatCode(() -> {
            HeliosSessionFactory factory = Helios.createSessionFactory(config);
            assertThat(factory).isNotNull();
            assertThat(factory.getConfiguration()).isEqualTo(config);
            factory.close(); // Close without opening session to avoid connection
        }).doesNotThrowAnyException();
    }
}