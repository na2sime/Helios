package fr.nassime.helios.mongo;

import fr.nassime.helios.api.config.HeliosConfiguration;
import fr.nassime.helios.mongo.config.MongoConfiguration;
import fr.nassime.helios.mongo.session.MongoSession;
import fr.nassime.helios.mongo.session.MongoSessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Abstract base class for MongoDB integration tests.
 * Sets up TestContainers MongoDB instance and provides common test utilities.
 */
@Testcontainers
public abstract class AbstractMongoTest {
    
    @Container
    static MongoDBContainer mongoContainer = new MongoDBContainer("mongo:7.0")
            .withExposedPorts(27017);
    
    protected MongoSessionFactory sessionFactory;
    protected MongoSession session;
    
    @BeforeEach
    void setUp() {
        // Ensure container is started
        if (!mongoContainer.isRunning()) {
            mongoContainer.start();
        }
        
        // Create configuration
        String connectionString = mongoContainer.getConnectionString();
        String databaseName = "test_helios";
        
        MongoConfiguration config = MongoConfiguration.builder()
                .connectionString(connectionString)
                .database(databaseName)
                .build();
        
        // Create session factory and session
        sessionFactory = new MongoSessionFactory(config);
        session = (MongoSession) sessionFactory.openSession();
    }
    
    @AfterEach
    void tearDown() {
        if (session != null) {
            session.close();
        }
        if (sessionFactory != null) {
            sessionFactory.close();
        }
    }
    
    /**
     * Clear all collections for clean test state.
     */
    protected void clearDatabase() {
        // Drop all collections
        for (String collectionName : session.getDatabase().listCollectionNames()) {
            session.getDatabase().getCollection(collectionName).drop();
        }
    }
}