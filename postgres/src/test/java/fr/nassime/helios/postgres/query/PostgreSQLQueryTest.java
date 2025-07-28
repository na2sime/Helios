package fr.nassime.helios.postgres.query;

import fr.nassime.helios.api.query.Query;
import fr.nassime.helios.api.query.QueryOperator;
import fr.nassime.helios.api.query.SortDirection;
import fr.nassime.helios.postgres.AbstractPostgreSQLTest;
import fr.nassime.helios.postgres.entities.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for PostgreSQLQuery.
 */
class PostgreSQLQueryTest extends AbstractPostgreSQLTest {
    
    @BeforeEach
    void setupTestData() {
        // Create test users
        User user1 = new User("alice", "alice@example.com");
        user1.setFullName("Alice Johnson");
        user1.setAge(25);
        user1.setActive(true);
        session.save(user1);
        
        User user2 = new User("bob", "bob@example.com");
        user2.setFullName("Bob Smith");
        user2.setAge(30);
        user2.setActive(true);
        session.save(user2);
        
        User user3 = new User("charlie", "charlie@example.com");
        user3.setFullName("Charlie Brown");
        user3.setAge(35);
        user3.setActive(false);
        session.save(user3);
        
        User user4 = new User("diana", "diana@example.com");
        user4.setFullName("Diana Prince");
        user4.setAge(28);
        user4.setActive(true);
        session.save(user4);
        
        User user5 = new User("eve", "eve@example.com");
        user5.setFullName("Eve Davis");
        user5.setAge(32);
        user5.setActive(true);
        session.save(user5);
    }
    
    @Test
    void testSimpleWhereQuery() {
        List<User> results = session.createQuery(User.class)
                .where("username", "alice")
                .getResultList();
        
        assertEquals(1, results.size());
        assertEquals("alice", results.get(0).getUsername());
        assertEquals("Alice Johnson", results.get(0).getFullName());
    }
    
    @Test
    void testWhereWithOperator() {
        List<User> results = session.createQuery(User.class)
                .where("age", QueryOperator.GREATER_THAN, 30)
                .getResultList();
        
        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(u -> u.getAge() > 30));
        assertTrue(results.stream().anyMatch(u -> "charlie".equals(u.getUsername())));
        assertTrue(results.stream().anyMatch(u -> "eve".equals(u.getUsername())));
    }
    
    @Test
    void testMultipleWhereConditions() {
        List<User> results = session.createQuery(User.class)
                .where("age", QueryOperator.GREATER_THAN, 25)
                .and("active", true)
                .getResultList();
        
        assertEquals(3, results.size());
        assertTrue(results.stream().allMatch(u -> u.getAge() > 25 && u.getActive()));
        assertFalse(results.stream().anyMatch(u -> "alice".equals(u.getUsername())));
        assertFalse(results.stream().anyMatch(u -> "charlie".equals(u.getUsername())));
    }
    
    @Test
    void testOrCondition() {
        List<User> results = session.createQuery(User.class)
                .where("username", "alice")
                .or("username", "bob")
                .getResultList();
        
        assertEquals(2, results.size());
        assertTrue(results.stream().anyMatch(u -> "alice".equals(u.getUsername())));
        assertTrue(results.stream().anyMatch(u -> "bob".equals(u.getUsername())));
    }
    
    @Test
    void testLikeOperator() {
        List<User> results = session.createQuery(User.class)
                .where("full_name", QueryOperator.LIKE, "%Johnson%")
                .getResultList();
        
        assertEquals(1, results.size());
        assertEquals("alice", results.get(0).getUsername());
        assertTrue(results.get(0).getFullName().contains("Johnson"));
    }
    
    @Test
    void testInOperator() {
        List<String> usernames = Arrays.asList("alice", "bob", "eve");
        
        List<User> results = session.createQuery(User.class)
                .where("username", QueryOperator.IN, usernames)
                .getResultList();
        
        assertEquals(3, results.size());
        assertTrue(results.stream().allMatch(u -> usernames.contains(u.getUsername())));
    }
    
    @Test
    void testBetweenOperator() {
        Object[] ageRange = {27, 32};
        
        List<User> results = session.createQuery(User.class)
                .where("age", QueryOperator.BETWEEN, ageRange)
                .getResultList();
        
        assertEquals(3, results.size());
        assertTrue(results.stream().allMatch(u -> u.getAge() >= 27 && u.getAge() <= 32));
    }
    
    @Test
    void testIsNullOperator() {
        // First, create a user with null full_name
        User userWithNull = new User("null_user", "null@example.com");
        userWithNull.setFullName(null);
        session.save(userWithNull);
        
        List<User> results = session.createQuery(User.class)
                .where("full_name", QueryOperator.IS_NULL, null)
                .getResultList();
        
        assertEquals(1, results.size());
        assertEquals("null_user", results.get(0).getUsername());
        assertNull(results.get(0).getFullName());
    }
    
    @Test
    void testIsNotNullOperator() {
        List<User> results = session.createQuery(User.class)
                .where("full_name", QueryOperator.IS_NOT_NULL, null)
                .getResultList();
        
        assertEquals(5, results.size()); // All test users have full names
        assertTrue(results.stream().allMatch(u -> u.getFullName() != null));
    }
    
    @Test
    void testOrderByAscending() {
        List<User> results = session.createQuery(User.class)
                .where("active", true)
                .orderBy("age", SortDirection.ASC)
                .getResultList();
        
        assertEquals(4, results.size());
        assertEquals("alice", results.get(0).getUsername()); // age 25
        assertEquals("diana", results.get(1).getUsername()); // age 28
        assertEquals("bob", results.get(2).getUsername());   // age 30
        assertEquals("eve", results.get(3).getUsername());   // age 32
    }
    
    @Test
    void testOrderByDescending() {
        List<User> results = session.createQuery(User.class)
                .where("active", true)
                .orderBy("age", SortDirection.DESC)
                .getResultList();
        
        assertEquals(4, results.size());
        assertEquals("eve", results.get(0).getUsername());   // age 32
        assertEquals("bob", results.get(1).getUsername());   // age 30
        assertEquals("diana", results.get(2).getUsername()); // age 28
        assertEquals("alice", results.get(3).getUsername()); // age 25
    }
    
    @Test
    void testMultipleOrderBy() {
        // Create users with same age to test secondary sort
        User sameAge1 = new User("same1", "same1@example.com");
        sameAge1.setAge(30);
        sameAge1.setFullName("Adam");
        session.save(sameAge1);
        
        User sameAge2 = new User("same2", "same2@example.com");
        sameAge2.setAge(30);
        sameAge2.setFullName("Zack");
        session.save(sameAge2);
        
        List<User> results = session.createQuery(User.class)
                .where("age", 30)
                .orderBy("age", SortDirection.ASC)
                .orderBy("full_name", SortDirection.ASC)
                .getResultList();
        
        assertEquals(3, results.size());
        assertEquals("Adam", results.get(0).getFullName());
        assertEquals("Bob Smith", results.get(1).getFullName());
        assertEquals("Zack", results.get(2).getFullName());
    }
    
    @Test
    void testLimitQuery() {
        List<User> results = session.createQuery(User.class)
                .orderBy("username")
                .limit(3)
                .getResultList();
        
        assertEquals(3, results.size());
        assertEquals("alice", results.get(0).getUsername());
        assertEquals("bob", results.get(1).getUsername());
        assertEquals("charlie", results.get(2).getUsername());
    }
    
    @Test
    void testOffsetQuery() {
        List<User> results = session.createQuery(User.class)
                .orderBy("username")
                .offset(2)
                .getResultList();
        
        assertEquals(3, results.size());
        assertEquals("charlie", results.get(0).getUsername());
        assertEquals("diana", results.get(1).getUsername());
        assertEquals("eve", results.get(2).getUsername());
    }
    
    @Test
    void testLimitAndOffset() {
        List<User> results = session.createQuery(User.class)
                .orderBy("username")
                .limit(2)
                .offset(1)
                .getResultList();
        
        assertEquals(2, results.size());
        assertEquals("bob", results.get(0).getUsername());
        assertEquals("charlie", results.get(1).getUsername());
    }
    
    @Test
    void testGetSingleResult() {
        Optional<User> result = session.createQuery(User.class)
                .where("username", "alice")
                .getSingleResult();
        
        assertTrue(result.isPresent());
        assertEquals("alice", result.get().getUsername());
    }
    
    @Test
    void testGetSingleResultNotFound() {
        Optional<User> result = session.createQuery(User.class)
                .where("username", "nonexistent")
                .getSingleResult();
        
        assertFalse(result.isPresent());
    }
    
    @Test
    void testGetSingleResultMultipleResults() {
        assertThrows(IllegalStateException.class, () -> {
            session.createQuery(User.class)
                    .where("active", true)
                    .getSingleResult();
        });
    }
    
    @Test
    void testGetFirstResult() {
        Optional<User> result = session.createQuery(User.class)
                .where("active", true)
                .orderBy("username")
                .getFirstResult();
        
        assertTrue(result.isPresent());
        assertEquals("alice", result.get().getUsername());
    }
    
    @Test
    void testGetFirstResultNotFound() {
        Optional<User> result = session.createQuery(User.class)
                .where("username", "nonexistent")
                .getFirstResult();
        
        assertFalse(result.isPresent());
    }
    
    @Test
    void testCountQuery() {
        long count = session.createQuery(User.class)
                .where("active", true)
                .count();
        
        assertEquals(4, count);
    }
    
    @Test
    void testCountWithConditions() {
        long count = session.createQuery(User.class)
                .where("age", QueryOperator.GREATER_THAN, 28)
                .count();
        
        assertEquals(3, count);
    }
    
    @Test
    void testComplexQuery() {
        List<User> results = session.createQuery(User.class)
                .where("active", true)
                .and("age", QueryOperator.GREATER_THAN_OR_EQUAL, 28)
                .or("username", "alice")
                .orderBy("age", SortDirection.DESC)
                .limit(10)
                .getResultList();
        
        // Should get alice (age 25, active) + diana (age 28, active) + bob (age 30, active) + eve (age 32, active)
        assertEquals(4, results.size());
        
        // Verify order (DESC by age)
        assertEquals("eve", results.get(0).getUsername());   // age 32
        assertEquals("bob", results.get(1).getUsername());   // age 30
        assertEquals("diana", results.get(2).getUsername()); // age 28
        assertEquals("alice", results.get(3).getUsername()); // age 25
    }
    
    @Test
    void testQueryWithNoResults() {
        List<User> results = session.createQuery(User.class)
                .where("username", "nonexistent")
                .getResultList();
        
        assertTrue(results.isEmpty());
    }
    
    @Test
    void testBetweenOperatorWithInvalidRange() {
        assertThrows(IllegalArgumentException.class, () -> {
            session.createQuery(User.class)
                    .where("age", QueryOperator.BETWEEN, new Object[]{25}) // Only one value
                    .getResultList();
        });
    }
    
    @Test
    void testNotBetweenOperator() {
        Object[] ageRange = {27, 32};
        
        List<User> results = session.createQuery(User.class)
                .where("age", QueryOperator.NOT_BETWEEN, ageRange)
                .getResultList();
        
        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(u -> u.getAge() < 27 || u.getAge() > 32));
        assertTrue(results.stream().anyMatch(u -> "alice".equals(u.getUsername()))); // age 25
        assertTrue(results.stream().anyMatch(u -> "charlie".equals(u.getUsername()))); // age 35
    }
    
    @Test
    void testNotEqualsOperator() {
        List<User> results = session.createQuery(User.class)
                .where("username", QueryOperator.NOT_EQUALS, "alice")
                .orderBy("username")
                .getResultList();
        
        assertEquals(4, results.size());
        assertFalse(results.stream().anyMatch(u -> "alice".equals(u.getUsername())));
    }
    
    @Test
    void testNotInOperator() {
        List<String> excludedUsernames = Arrays.asList("alice", "bob");
        
        List<User> results = session.createQuery(User.class)
                .where("username", QueryOperator.NOT_IN, excludedUsernames)
                .orderBy("username")
                .getResultList();
        
        assertEquals(3, results.size());
        assertTrue(results.stream().noneMatch(u -> excludedUsernames.contains(u.getUsername())));
        assertTrue(results.stream().anyMatch(u -> "charlie".equals(u.getUsername())));
        assertTrue(results.stream().anyMatch(u -> "diana".equals(u.getUsername())));
        assertTrue(results.stream().anyMatch(u -> "eve".equals(u.getUsername())));
    }
}