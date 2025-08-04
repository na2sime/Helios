package fr.nassime.helios.mariadb;

import fr.nassime.helios.api.HeliosSession;
import fr.nassime.helios.api.HeliosSessionFactory;
import fr.nassime.helios.mariadb.config.MariaDBConfiguration;
import fr.nassime.helios.mariadb.session.MariaDBSessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for MariaDB integration tests using TestContainers.
 * These tests require Docker to be available.
 */
@Testcontainers
@DockerTestHelper.EnabledIfDockerAvailable
public abstract class AbstractMariaDBTest {
    
    @Container
    static MariaDBContainer<?> mariadb = new MariaDBContainer<>("mariadb:11.0")
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
            mariadb.start();
            if (!mariadb.isRunning()) {
                throw new RuntimeException("MariaDB container failed to start");
            }
            
            // Create session factory
            MariaDBConfiguration config = MariaDBConfiguration.builder()
                    .host(mariadb.getHost())
                    .port(mariadb.getFirstMappedPort())
                    .database(mariadb.getDatabaseName())
                    .username(mariadb.getUsername())
                    .password(mariadb.getPassword())
                    .build();
            
            sessionFactory = new MariaDBSessionFactory(config);
            
            // Create database schema
            createDatabaseSchema();
        } catch (Exception e) {
            System.err.println("Failed to setup test database: " + e.getMessage());
            throw new RuntimeException("Cannot run integration tests without MariaDB container", e);
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
            session.executeUpdate("""
                CREATE TABLE IF NOT EXISTS users (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    username VARCHAR(50) NOT NULL UNIQUE,
                    email VARCHAR(100) NOT NULL,
                    full_name VARCHAR(100),
                    age INT,
                    active BOOLEAN NOT NULL DEFAULT TRUE,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """);
            
            // Create user_profiles table
            session.executeUpdate("""
                CREATE TABLE IF NOT EXISTS user_profiles (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    user_id INT UNIQUE,
                    bio TEXT,
                    website VARCHAR(255),
                    location VARCHAR(100),
                    avatar_url VARCHAR(255),
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                )
                """);
            
            // Create posts table
            session.executeUpdate("""
                CREATE TABLE IF NOT EXISTS posts (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    title VARCHAR(200) NOT NULL,
                    content TEXT NOT NULL,
                    published BOOLEAN NOT NULL DEFAULT FALSE,
                    view_count INT DEFAULT 0,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP,
                    author_id INT,
                    FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE
                )
                """);
            
            // Create comments table
            session.executeUpdate("""
                CREATE TABLE IF NOT EXISTS comments (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    content TEXT NOT NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    post_id INT,
                    author_id INT,
                    FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
                    FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE
                )
                """);
            
            // Create tags table
            session.executeUpdate("""
                CREATE TABLE IF NOT EXISTS tags (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(50) NOT NULL UNIQUE,
                    description TEXT,
                    color VARCHAR(7)
                )
                """);
            
            // Create post_tags junction table
            session.executeUpdate("""
                CREATE TABLE IF NOT EXISTS post_tags (
                    post_id INT,
                    tag_id INT,
                    PRIMARY KEY (post_id, tag_id),
                    FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
                    FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
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