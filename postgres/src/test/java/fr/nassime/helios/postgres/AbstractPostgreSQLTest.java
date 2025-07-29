package fr.nassime.helios.postgres;

import fr.nassime.helios.api.HeliosSession;
import fr.nassime.helios.api.HeliosSessionFactory;
import fr.nassime.helios.postgres.config.PostgreSQLConfiguration;
import fr.nassime.helios.postgres.session.PostgreSQLSessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for PostgreSQL integration tests using TestContainers.
 * These tests require Docker to be available.
 */
@Testcontainers
@DockerTestHelper.EnabledIfDockerAvailable
public abstract class AbstractPostgreSQLTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("helios_test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true)
            .withStartupTimeout(java.time.Duration.ofMinutes(5))
            .withConnectTimeoutSeconds(60);
    
    protected static HeliosSessionFactory sessionFactory;
    protected HeliosSession session;
    
    @BeforeAll
    static void setupDatabase() {
        try {
            // Wait for container to be ready
            postgres.start();
            if (!postgres.isRunning()) {
                throw new RuntimeException("PostgreSQL container failed to start");
            }
            
            // Create session factory
            PostgreSQLConfiguration config = PostgreSQLConfiguration.builder()
                    .host(postgres.getHost())
                    .port(postgres.getFirstMappedPort())
                    .database(postgres.getDatabaseName())
                    .username(postgres.getUsername())
                    .password(postgres.getPassword())
                    .build();
            
            sessionFactory = new PostgreSQLSessionFactory(config);
            
            // Create database schema
            createDatabaseSchema();
        } catch (Exception e) {
            System.err.println("Failed to setup test database: " + e.getMessage());
            throw new RuntimeException("Cannot run integration tests without PostgreSQL container", e);
        }
    }
    
    @BeforeEach
    void setUp() {
        session = sessionFactory.openSession();
    }
    
    @AfterEach
    void tearDown() {
        if (session != null) {
            session.close();
        }
        // Clean up data after each test
        cleanupTestData();
    }
    
    private static void createDatabaseSchema() {
        try (HeliosSession session = sessionFactory.openSession()) {
            System.out.println("Creating database schema...");
            
            // Create users table
            int result1 = session.executeUpdate("""
                CREATE TABLE IF NOT EXISTS users (
                    id SERIAL PRIMARY KEY,
                    username VARCHAR(50) NOT NULL UNIQUE,
                    email VARCHAR(100) NOT NULL,
                    full_name VARCHAR(100),
                    age INTEGER,
                    active BOOLEAN NOT NULL DEFAULT TRUE,
                    created_at TIMESTAMP NOT NULL DEFAULT NOW()
                )
                """);
            System.out.println("Created users table, result: " + result1);
            
            // Create user_profiles table
            session.executeUpdate("""
                CREATE TABLE IF NOT EXISTS user_profiles (
                    id SERIAL PRIMARY KEY,
                    user_id INTEGER UNIQUE REFERENCES users(id) ON DELETE CASCADE,
                    bio TEXT,
                    website VARCHAR(255),
                    location VARCHAR(100),
                    avatar_url VARCHAR(255)
                )
                """);
            
            // Create posts table
            session.executeUpdate("""
                CREATE TABLE IF NOT EXISTS posts (
                    id SERIAL PRIMARY KEY,
                    title VARCHAR(200) NOT NULL,
                    content TEXT NOT NULL,
                    published BOOLEAN NOT NULL DEFAULT FALSE,
                    view_count INTEGER DEFAULT 0,
                    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                    updated_at TIMESTAMP,
                    author_id INTEGER REFERENCES users(id) ON DELETE CASCADE
                )
                """);
            
            // Create comments table
            session.executeUpdate("""
                CREATE TABLE IF NOT EXISTS comments (
                    id SERIAL PRIMARY KEY,
                    content TEXT NOT NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                    post_id INTEGER REFERENCES posts(id) ON DELETE CASCADE,
                    author_id INTEGER REFERENCES users(id) ON DELETE CASCADE
                )
                """);
            
            // Create tags table
            session.executeUpdate("""
                CREATE TABLE IF NOT EXISTS tags (
                    id SERIAL PRIMARY KEY,
                    name VARCHAR(50) NOT NULL UNIQUE,
                    description TEXT,
                    color VARCHAR(7)
                )
                """);
            
            // Create post_tags junction table
            session.executeUpdate("""
                CREATE TABLE IF NOT EXISTS post_tags (
                    post_id INTEGER REFERENCES posts(id) ON DELETE CASCADE,
                    tag_id INTEGER REFERENCES tags(id) ON DELETE CASCADE,
                    PRIMARY KEY (post_id, tag_id)
                )
                """);
            
            session.flush();
            System.out.println("Database schema created successfully!");
        } catch (Exception e) {
            System.err.println("Failed to create database schema: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Schema creation failed", e);
        }
    }
    
    private void cleanupTestData() {
        try (HeliosSession cleanupSession = sessionFactory.openSession()) {
            // Delete in reverse order to avoid foreign key conflicts
            cleanupSession.executeUpdate("DELETE FROM post_tags");
            cleanupSession.executeUpdate("DELETE FROM comments");
            cleanupSession.executeUpdate("DELETE FROM posts");
            cleanupSession.executeUpdate("DELETE FROM user_profiles");
            cleanupSession.executeUpdate("DELETE FROM tags");
            cleanupSession.executeUpdate("DELETE FROM users");
            cleanupSession.flush();
        }
    }
}