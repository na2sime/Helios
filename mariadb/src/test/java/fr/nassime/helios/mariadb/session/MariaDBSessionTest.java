package fr.nassime.helios.mariadb.session;

import fr.nassime.helios.api.HeliosSession;
import fr.nassime.helios.api.exception.HeliosException;
import fr.nassime.helios.api.transaction.Transaction;
import fr.nassime.helios.mariadb.AbstractMariaDBTest;
import fr.nassime.helios.mariadb.entities.User;
import fr.nassime.helios.mariadb.entities.UserProfile;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for MariaDBSession.
 */
class MariaDBSessionTest extends AbstractMariaDBTest {
    
    @Test
    void testSaveAndFindById() {
        // Create and save a user
        User user = new User("john_doe", "john@example.com");
        user.setFullName("John Doe");
        user.setAge(25);
        
        User savedUser = session.save(user);
        
        // Verify user was saved with generated ID
        assertNotNull(savedUser.getId());
        assertEquals("john_doe", savedUser.getUsername());
        assertEquals("john@example.com", savedUser.getEmail());
        assertEquals("John Doe", savedUser.getFullName());
        assertEquals(25, savedUser.getAge());
        assertTrue(savedUser.getActive());
        assertNotNull(savedUser.getCreatedAt());
        
        // Find by ID
        Optional<User> foundUser = session.findById(User.class, savedUser.getId());
        assertTrue(foundUser.isPresent());
        assertEquals(savedUser.getId(), foundUser.get().getId());
        assertEquals(savedUser.getUsername(), foundUser.get().getUsername());
        assertEquals(savedUser.getEmail(), foundUser.get().getEmail());
    }
    
    @Test
    void testFindByIdNotFound() {
        Optional<User> foundUser = session.findById(User.class, 999L);
        assertFalse(foundUser.isPresent());
    }
    
    @Test
    void testFindByIdWithNullId() {
        Optional<User> foundUser = session.findById(User.class, null);
        assertFalse(foundUser.isPresent());
    }
    
    @Test
    void testUpdateEntity() {
        // Create and save a user
        User user = new User("jane_doe", "jane@example.com");
        user.setAge(30);
        User savedUser = session.save(user);
        
        // Update the user
        savedUser.setFullName("Jane Smith");
        savedUser.setAge(31);
        savedUser.setEmail("jane.smith@example.com");
        
        User updatedUser = session.save(savedUser);
        
        // Verify updates
        assertEquals(savedUser.getId(), updatedUser.getId());
        assertEquals("Jane Smith", updatedUser.getFullName());
        assertEquals(31, updatedUser.getAge());
        assertEquals("jane.smith@example.com", updatedUser.getEmail());
        
        // Verify in database
        Optional<User> foundUser = session.findById(User.class, savedUser.getId());
        assertTrue(foundUser.isPresent());
        assertEquals("Jane Smith", foundUser.get().getFullName());
        assertEquals(31, foundUser.get().getAge());
    }
    
    @Test
    void testDeleteEntity() {
        // Create and save a user
        User user = new User("to_delete", "delete@example.com");
        User savedUser = session.save(user);
        Long userId = savedUser.getId();
        
        // Delete the user
        boolean deleted = session.delete(savedUser);
        assertTrue(deleted);
        
        // Verify user is deleted
        Optional<User> foundUser = session.findById(User.class, userId);
        assertFalse(foundUser.isPresent());
    }
    
    @Test
    void testDeleteNonExistentEntity() {
        User user = new User("non_existent", "non@example.com");
        user.setId(999L);
        
        boolean deleted = session.delete(user);
        assertFalse(deleted);
    }
    
    @Test
    void testDeleteEntityWithoutId() {
        User user = new User("no_id", "noid@example.com");
        
        assertThrows(HeliosException.class, () -> session.delete(user));
    }
    
    @Test
    void testFindAll() {
        // Create multiple users
        User user1 = new User("user1", "user1@example.com");
        User user2 = new User("user2", "user2@example.com");
        User user3 = new User("user3", "user3@example.com");
        
        session.save(user1);
        session.save(user2);
        session.save(user3);
        
        // Find all users
        List<User> allUsers = session.findAll(User.class);
        
        assertEquals(3, allUsers.size());
        assertTrue(allUsers.stream().anyMatch(u -> "user1".equals(u.getUsername())));
        assertTrue(allUsers.stream().anyMatch(u -> "user2".equals(u.getUsername())));
        assertTrue(allUsers.stream().anyMatch(u -> "user3".equals(u.getUsername())));
    }
    
    @Test
    void testTransactionCommit() {
        Transaction tx = session.beginTransaction();
        
        try {
            User user = new User("tx_user", "tx@example.com");
            session.save(user);
            
            tx.commit();
            
            // Verify user exists after commit
            List<User> users = session.findAll(User.class);
            assertEquals(1, users.size());
            assertEquals("tx_user", users.get(0).getUsername());
            
        } catch (Exception e) {
            tx.rollback();
            fail("Transaction should not have failed");
        }
    }
    
    @Test
    void testTransactionRollback() {
        // Save initial user
        User initialUser = new User("initial", "initial@example.com");
        session.save(initialUser);
        
        Transaction tx = session.beginTransaction();
        
        try {
            User user = new User("rollback_user", "rollback@example.com");
            session.save(user);
            
            // Force rollback
            tx.rollback();
            
            // Verify rollback user doesn't exist
            List<User> users = session.findAll(User.class);
            assertEquals(1, users.size());
            assertEquals("initial", users.get(0).getUsername());
            
        } catch (Exception e) {
            tx.rollback();
        }
    }
    
    @Test
    void testExecuteInTransaction() {
        User result = session.executeInTransaction(s -> {
            User user = new User("tx_lambda", "lambda@example.com");
            return s.save(user);
        });
        
        assertNotNull(result.getId());
        assertEquals("tx_lambda", result.getUsername());
        
        // Verify user exists
        Optional<User> foundUser = session.findById(User.class, result.getId());
        assertTrue(foundUser.isPresent());
    }
    
    @Test
    void testExecuteInTransactionWithException() {
        assertThrows(RuntimeException.class, () -> {
            session.executeInTransaction((HeliosSession s) -> {
                User user = new User("exception_user", "exception@example.com");
                s.save(user);
                throw new RuntimeException("Test exception");
            });
        });
        
        // Verify no user was saved due to rollback
        List<User> users = session.findAll(User.class);
        assertEquals(0, users.size());
    }
    
    @Test
    void testFlush() {
        Transaction tx = session.beginTransaction();
        
        User user = new User("flush_user", "flush@example.com");
        session.save(user);
        
        // Flush should commit the transaction
        session.flush();
        
        // Verify user exists
        List<User> users = session.findAll(User.class);
        assertEquals(1, users.size());
        assertEquals("flush_user", users.get(0).getUsername());
    }
    
    @Test
    void testClear() {
        Transaction tx = session.beginTransaction();
        
        User user = new User("clear_user", "clear@example.com");
        session.save(user);
        
        // Clear should rollback the transaction
        session.clear();
        
        // Verify user doesn't exist (was rolled back)
        List<User> users = session.findAll(User.class);
        assertEquals(0, users.size());
    }
    
    @Test
    void testNativeQuery() {
        // Create test users
        User user1 = new User("native1", "native1@example.com");
        user1.setAge(25);
        User user2 = new User("native2", "native2@example.com");
        user2.setAge(35);
        
        session.save(user1);
        session.save(user2);
        
        // Execute native query
        List<User> adults = session.executeNativeQuery(
            "SELECT * FROM users WHERE age >= ?", 
            User.class, 
            30
        );
        
        assertEquals(1, adults.size());
        assertEquals("native2", adults.get(0).getUsername());
        assertEquals(35, adults.get(0).getAge());
    }
    
    @Test
    void testExecuteUpdate() {
        // Create test users
        User user1 = new User("update1", "update1@example.com");
        user1.setAge(25);
        User user2 = new User("update2", "update2@example.com");
        user2.setAge(35);
        
        session.save(user1);
        session.save(user2);
        
        // Execute update query
        int updatedRows = session.executeUpdate(
            "UPDATE users SET age = age + 1 WHERE age >= ?", 
            30
        );
        
        assertEquals(1, updatedRows);
        
        // Verify update
        Optional<User> updatedUser = session.findById(User.class, user2.getId());
        assertTrue(updatedUser.isPresent());
        assertEquals(36, updatedUser.get().getAge());
    }
    
    @Test
    void testSaveWithNullEntity() {
        assertThrows(IllegalArgumentException.class, () -> session.save(null));
    }
    
    @Test
    void testDeleteWithNullEntity() {
        assertThrows(IllegalArgumentException.class, () -> session.delete(null));
    }
    
    @Test
    void testLoadRelationWithNullEntity() {
        assertThrows(IllegalArgumentException.class, () -> session.loadRelation(null, "posts"));
    }
}