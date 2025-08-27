package fr.nassime.helios.mongo;

import fr.nassime.helios.api.HeliosSessionFactory;
import fr.nassime.helios.api.config.HeliosConfiguration;
import fr.nassime.helios.api.exception.HeliosException;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MongoProvider.
 */
class MongoProviderTest {
    
    private final MongoProvider provider = new MongoProvider();
    
    @Test
    void testGetName() {
        assertEquals("mongodb", provider.getName());
    }
    
    @Test
    void testCreateSessionFactoryWithValidConfiguration() {
        Map<String, String> properties = new HashMap<>();
        properties.put("mongo.connectionString", "mongodb://localhost:27017");
        properties.put("mongo.database", "test");
        
        HeliosConfiguration config = new TestHeliosConfiguration(properties);
        
        HeliosSessionFactory factory = provider.createSessionFactory(config);
        
        assertNotNull(factory);
        
        // Clean up
        factory.close();
    }
    
    @Test
    void testCreateSessionFactoryWithOptionalParameters() {
        Map<String, String> properties = new HashMap<>();
        properties.put("mongo.connectionString", "mongodb://localhost:27017");
        properties.put("mongo.database", "test");
        properties.put("mongo.connectionTimeoutMs", "5000");
        properties.put("mongo.socketTimeoutMs", "10000");
        properties.put("mongo.maxConnectionPoolSize", "50");
        
        HeliosConfiguration config = new TestHeliosConfiguration(properties);
        
        HeliosSessionFactory factory = provider.createSessionFactory(config);
        
        assertNotNull(factory);
        
        // Clean up
        factory.close();
    }
    
    @Test
    void testCreateSessionFactoryMissingConnectionString() {
        Map<String, String> properties = new HashMap<>();
        properties.put("mongo.database", "test");
        
        HeliosConfiguration config = new TestHeliosConfiguration(properties);
        
        HeliosException exception = assertThrows(HeliosException.class, 
            () -> provider.createSessionFactory(config));
        
        assertTrue(exception.getMessage().contains("connection string is required"));
    }
    
    @Test
    void testCreateSessionFactoryMissingDatabase() {
        Map<String, String> properties = new HashMap<>();
        properties.put("mongo.connectionString", "mongodb://localhost:27017");
        
        HeliosConfiguration config = new TestHeliosConfiguration(properties);
        
        HeliosException exception = assertThrows(HeliosException.class, 
            () -> provider.createSessionFactory(config));
        
        assertTrue(exception.getMessage().contains("database name is required"));
    }
    
    @Test
    void testCreateSessionFactoryEmptyConnectionString() {
        Map<String, String> properties = new HashMap<>();
        properties.put("mongo.connectionString", "");
        properties.put("mongo.database", "test");
        
        HeliosConfiguration config = new TestHeliosConfiguration(properties);
        
        HeliosException exception = assertThrows(HeliosException.class, 
            () -> provider.createSessionFactory(config));
        
        assertTrue(exception.getMessage().contains("connection string is required"));
    }
    
    @Test
    void testCreateSessionFactoryEmptyDatabase() {
        Map<String, String> properties = new HashMap<>();
        properties.put("mongo.connectionString", "mongodb://localhost:27017");
        properties.put("mongo.database", "");
        
        HeliosConfiguration config = new TestHeliosConfiguration(properties);
        
        HeliosException exception = assertThrows(HeliosException.class, 
            () -> provider.createSessionFactory(config));
        
        assertTrue(exception.getMessage().contains("database name is required"));
    }
    
    /**
     * Test implementation of HeliosConfiguration.
     */
    private static class TestHeliosConfiguration implements HeliosConfiguration {
        private final Map<String, String> properties;
        
        public TestHeliosConfiguration(Map<String, String> properties) {
            this.properties = properties;
        }
        
        @Override
        public String getProperty(String key) {
            return properties.get(key);
        }
        
        @Override
        public String getProperty(String key, String defaultValue) {
            return properties.getOrDefault(key, defaultValue);
        }
        
        @Override
        public Map<String, String> getAllProperties() {
            return new HashMap<>(properties);
        }
        
        @Override
        public boolean hasProperty(String key) {
            return properties.containsKey(key);
        }
    }
}