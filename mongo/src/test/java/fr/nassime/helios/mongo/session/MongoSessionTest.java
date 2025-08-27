package fr.nassime.helios.mongo.session;

import fr.nassime.helios.api.query.Query;
import fr.nassime.helios.api.query.QueryOperator;
import fr.nassime.helios.api.query.SortDirection;
import fr.nassime.helios.mongo.AbstractMongoTest;
import fr.nassime.helios.mongo.entities.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for MongoSession CRUD operations and queries.
 */
class MongoSessionTest extends AbstractMongoTest {
    
    @BeforeEach
    void setUpTest() {
        clearDatabase();
    }
    
    @Test
    void testSaveAndFindById() {
        // Create user
        User user = new User("john_doe", "john@example.com", 25, true);
        
        // Save user
        User savedUser = session.save(user);
        
        assertNotNull(savedUser);
        assertNotNull(savedUser.getId());
        assertEquals("john_doe", savedUser.getUsername());
        assertEquals("john@example.com", savedUser.getEmail());
        assertEquals(25, savedUser.getAge());
        assertTrue(savedUser.getActive());
        
        // Find by ID
        Optional<User> foundUserOpt = session.findById(User.class, savedUser.getId());
        
        assertTrue(foundUserOpt.isPresent());
        User foundUser = foundUserOpt.get();
        assertEquals(savedUser.getId(), foundUser.getId());
        assertEquals("john_doe", foundUser.getUsername());
        assertEquals("john@example.com", foundUser.getEmail());
        assertEquals(25, foundUser.getAge());
        assertTrue(foundUser.getActive());
    }
    
    @Test
    void testFindByIdNotFound() {
        Optional<User> notFound = session.findById(User.class, "nonexistent_id");
        assertFalse(notFound.isPresent());
    }
    
    @Test
    void testSaveUpdate() {
        // Create and save user
        User user = new User("jane_doe", "jane@example.com", 30, true);
        User savedUser = session.save(user);
        String originalId = savedUser.getId();
        
        // Update user
        savedUser.setEmail("jane.updated@example.com");
        savedUser.setAge(31);
        
        User updatedUser = session.save(savedUser);
        
        assertNotNull(updatedUser);
        assertEquals(originalId, updatedUser.getId()); // ID should remain the same
        assertEquals("jane_doe", updatedUser.getUsername());
        assertEquals("jane.updated@example.com", updatedUser.getEmail());
        assertEquals(31, updatedUser.getAge());
        assertTrue(updatedUser.getActive());
        
        // Verify in database
        Optional<User> foundUserOpt = session.findById(User.class, originalId);
        assertTrue(foundUserOpt.isPresent());
        User foundUser = foundUserOpt.get();
        assertEquals("jane.updated@example.com", foundUser.getEmail());
        assertEquals(31, foundUser.getAge());
    }
    
    @Test
    void testSaveAll() {
        List<User> users = Arrays.asList(
                new User("user1", "user1@example.com", 25, true),
                new User("user2", "user2@example.com", 30, false),
                new User("user3", "user3@example.com", 35, true)
        );
        
        List<User> savedUsers = session.saveAll(users);
        
        assertNotNull(savedUsers);
        assertEquals(3, savedUsers.size());
        
        // Verify all have IDs
        for (User user : savedUsers) {
            assertNotNull(user.getId());
        }
        
        // Verify in database
        List<User> allUsers = session.findAll(User.class);
        assertEquals(3, allUsers.size());
    }
    
    @Test
    void testFindAll() {
        // Create test data
        session.save(new User("user1", "user1@example.com", 25, true));
        session.save(new User("user2", "user2@example.com", 30, false));
        session.save(new User("user3", "user3@example.com", 35, true));
        
        List<User> allUsers = session.findAll(User.class);
        
        assertNotNull(allUsers);
        assertEquals(3, allUsers.size());
    }
    
    @Test
    void testDeleteById() {
        // Create and save user
        User user = new User("to_delete", "delete@example.com", 25, true);
        User savedUser = session.save(user);
        String userId = savedUser.getId();
        
        // Verify user exists
        Optional<User> foundUserOpt = session.findById(User.class, userId);
        assertTrue(foundUserOpt.isPresent());
        
        // Delete user
        session.deleteById(User.class, userId);
        
        // Verify user is deleted
        Optional<User> deletedUserOpt = session.findById(User.class, userId);
        assertFalse(deletedUserOpt.isPresent());
    }
    
    @Test
    void testDelete() {
        // Create and save user
        User user = new User("to_delete", "delete@example.com", 25, true);
        User savedUser = session.save(user);
        
        // Delete user
        session.delete(savedUser);
        
        // Verify user is deleted
        Optional<User> deletedUserOpt = session.findById(User.class, savedUser.getId());
        assertFalse(deletedUserOpt.isPresent());
    }
    
    @Test
    void testDeleteAll() {
        // Create test data
        session.save(new User("user1", "user1@example.com", 25, true));
        session.save(new User("user2", "user2@example.com", 30, false));
        session.save(new User("user3", "user3@example.com", 35, true));
        
        // Verify users exist
        List<User> allUsers = session.findAll(User.class);
        assertEquals(3, allUsers.size());
        
        // Delete all users
        session.deleteAll(User.class);
        
        // Verify all users are deleted
        List<User> remainingUsers = session.findAll(User.class);
        assertEquals(0, remainingUsers.size());
    }
    
    @Test
    void testCreateQueryBasic() {
        // Create test data
        session.save(new User("john", "john@example.com", 25, true));
        session.save(new User("jane", "jane@example.com", 30, false));
        session.save(new User("bob", "bob@example.com", 35, true));
        
        // Query for active users
        Query<User> query = session.createQuery(User.class)
                .where("active", true);
        
        List<User> activeUsers = query.getResultList();
        
        assertNotNull(activeUsers);
        assertEquals(2, activeUsers.size());
        assertTrue(activeUsers.stream().allMatch(User::getActive));
    }
    
    @Test
    void testCreateQueryWithOperators() {
        // Create test data
        session.save(new User("user1", "user1@example.com", 20, true));
        session.save(new User("user2", "user2@example.com", 30, true));
        session.save(new User("user3", "user3@example.com", 40, true));
        
        // Query for users older than 25
        Query<User> query = session.createQuery(User.class)
                .where("age", QueryOperator.GREATER_THAN, 25);
        
        List<User> olderUsers = query.getResultList();
        
        assertNotNull(olderUsers);
        assertEquals(2, olderUsers.size());
        assertTrue(olderUsers.stream().allMatch(u -> u.getAge() > 25));
    }
    
    @Test
    void testCreateQueryWithSorting() {
        // Create test data
        session.save(new User("charlie", "charlie@example.com", 35, true));
        session.save(new User("alice", "alice@example.com", 25, true));
        session.save(new User("bob", "bob@example.com", 30, true));
        
        // Query with sorting by age descending
        Query<User> query = session.createQuery(User.class)
                .orderBy("age", SortDirection.DESC);
        
        List<User> sortedUsers = query.getResultList();
        
        assertNotNull(sortedUsers);
        assertEquals(3, sortedUsers.size());
        assertEquals(35, sortedUsers.get(0).getAge());
        assertEquals(30, sortedUsers.get(1).getAge());
        assertEquals(25, sortedUsers.get(2).getAge());
    }
    
    @Test
    void testCreateQueryWithLimitAndOffset() {
        // Create test data
        for (int i = 1; i <= 10; i++) {
            session.save(new User("user" + i, "user" + i + "@example.com", 20 + i, true));
        }
        
        // Query with limit and offset
        Query<User> query = session.createQuery(User.class)
                .orderBy("age")
                .offset(3)
                .limit(4);
        
        List<User> pagedUsers = query.getResultList();
        
        assertNotNull(pagedUsers);
        assertEquals(4, pagedUsers.size());
        assertEquals(24, pagedUsers.get(0).getAge()); // Should start from 4th user (age 24)
    }
    
    @Test
    void testCreateQueryCount() {
        // Create test data
        session.save(new User("user1", "user1@example.com", 25, true));
        session.save(new User("user2", "user2@example.com", 30, false));
        session.save(new User("user3", "user3@example.com", 35, true));
        
        // Count all users
        Query<User> allQuery = session.createQuery(User.class);
        long totalCount = allQuery.count();
        assertEquals(3, totalCount);
        
        // Count active users
        Query<User> activeQuery = session.createQuery(User.class)
                .where("active", true);
        long activeCount = activeQuery.count();
        assertEquals(2, activeCount);
    }
    
    @Test
    void testCreateQuerySingleResult() {
        // Create test data
        session.save(new User("unique_user", "unique@example.com", 25, true));
        
        // Get single result
        Query<User> query = session.createQuery(User.class)
                .where("username", "unique_user");
        
        Optional<User> singleResult = query.getSingleResult();
        
        assertTrue(singleResult.isPresent());
        assertEquals("unique_user", singleResult.get().getUsername());
    }
    
    @Test
    void testCreateQueryFirstResult() {
        // Create test data
        session.save(new User("user1", "user1@example.com", 25, true));
        session.save(new User("user2", "user2@example.com", 30, true));
        session.save(new User("user3", "user3@example.com", 35, true));
        
        // Get first result
        Query<User> query = session.createQuery(User.class)
                .where("active", true)
                .orderBy("age");
        
        Optional<User> firstResult = query.getFirstResult();
        
        assertTrue(firstResult.isPresent());
        assertEquals(25, firstResult.get().getAge()); // Youngest active user
    }
}