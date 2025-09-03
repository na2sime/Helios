package fr.nassime.helios.mongo.repository;

import fr.nassime.helios.api.repository.Repository;
import fr.nassime.helios.api.repository.RepositoryFactory;
import fr.nassime.helios.mongo.AbstractMongoTest;
import fr.nassime.helios.mongo.entities.User;
import fr.nassime.helios.mongo.entities.Department;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Repository pattern with MongoDB.
 */
class MongoRepositoryIntegrationTest extends AbstractMongoTest {
    
    private Repository<User, String> userRepository;
    private Repository<Department, String> departmentRepository;
    private RepositoryFactory repositoryFactory;
    
    @BeforeEach
    void setUpRepositories() {
        // The session is already initialized by AbstractMongoTest.setUp()
        if (session == null) {
            throw new IllegalStateException("Session should be initialized by AbstractMongoTest");
        }
        repositoryFactory = new RepositoryFactory(session);
        userRepository = repositoryFactory.getRepository(User.class, String.class);
        departmentRepository = repositoryFactory.getRepository(Department.class, String.class);
        
        // Clean up data before each test
        userRepository.deleteAll();
        departmentRepository.deleteAll();
    }
    
    @Test
    void testMongoRepositoryBasicOperations() {
        // Test save
        User user = new User();
        user.setUsername("mongo_repo_user");
        user.setEmail("mongoRepo@example.com");
        user.setAge(28);
        user.setActive(true);
        
        User savedUser = userRepository.save(user);
        assertNotNull(savedUser.getId());
        assertEquals("mongo_repo_user", savedUser.getUsername());
        
        // Test findById
        Optional<User> foundUser = userRepository.findById(savedUser.getId());
        assertTrue(foundUser.isPresent());
        assertEquals(savedUser.getId(), foundUser.get().getId());
        assertEquals("mongo_repo_user", foundUser.get().getUsername());
        
        // Test existsById
        assertTrue(userRepository.existsById(savedUser.getId()));
        assertFalse(userRepository.existsById("nonexistent_id"));
        
        // Test update
        savedUser.setEmail("updated.mongoRepo@example.com");
        User updatedUser = userRepository.save(savedUser);
        assertEquals("updated.mongoRepo@example.com", updatedUser.getEmail());
        
        // Test delete
        boolean deleted = userRepository.delete(savedUser);
        assertTrue(deleted);
        
        // Verify deletion
        Optional<User> deletedUser = userRepository.findById(savedUser.getId());
        assertFalse(deletedUser.isPresent());
    }
    
    @Test
    void testMongoRepositoryBatchOperations() {
        // Create multiple users
        User user1 = new User();
        user1.setUsername("mongo_batch1");
        user1.setEmail("mongoBatch1@example.com");
        user1.setAge(25);
        
        User user2 = new User();
        user2.setUsername("mongo_batch2");
        user2.setEmail("mongoBatch2@example.com");
        user2.setAge(30);
        
        User user3 = new User();
        user3.setUsername("mongo_batch3");
        user3.setEmail("mongoBatch3@example.com");
        user3.setAge(35);
        
        List<User> users = Arrays.asList(user1, user2, user3);
        
        // Test saveAll
        List<User> savedUsers = userRepository.saveAll(users);
        assertEquals(3, savedUsers.size());
        savedUsers.forEach(user -> assertNotNull(user.getId()));
        
        // Test findAll
        List<User> allUsers = userRepository.findAll();
        assertEquals(3, allUsers.size());
        
        // Test count
        long count = userRepository.count();
        assertEquals(3, count);
        
        // Test deleteAll with specific entities
        userRepository.deleteAll(savedUsers.subList(0, 2));
        
        List<User> remainingUsers = userRepository.findAll();
        assertEquals(1, remainingUsers.size());
        assertEquals("mongo_batch3", remainingUsers.get(0).getUsername());
        
        // Test deleteAll (all remaining)
        userRepository.deleteAll();
        
        List<User> finalUsers = userRepository.findAll();
        assertEquals(0, finalUsers.size());
    }
    
    @Test
    void testMongoRepositoryDeleteById() {
        User user = new User();
        user.setUsername("mongo_delete_by_id");
        user.setEmail("mongoDeleteById@example.com");
        
        User savedUser = userRepository.save(user);
        String userId = savedUser.getId();
        
        // Test deleteById - existing
        boolean deleted = userRepository.deleteById(userId);
        assertTrue(deleted);
        
        // Verify deletion
        assertFalse(userRepository.existsById(userId));
        
        // Test deleteById - non-existing
        boolean notDeleted = userRepository.deleteById("nonexistent_id");
        assertFalse(notDeleted);
    }
    
    @Test
    void testMongoRepositoryQueryMethods() {
        // Create test data
        User user1 = new User();
        user1.setUsername("mongo_query1");
        user1.setEmail("mongoQuery1@example.com");
        user1.setAge(25);
        user1.setActive(true);
        
        User user2 = new User();
        user2.setUsername("mongo_query2");
        user2.setEmail("mongoQuery2@example.com");
        user2.setAge(35);
        user2.setActive(false);
        
        User user3 = new User();
        user3.setUsername("mongo_query3");
        user3.setEmail("mongoQuery3@example.com");
        user3.setAge(25);
        user3.setActive(true);
        
        userRepository.saveAll(Arrays.asList(user1, user2, user3));
        
        // Test findByField
        List<User> activeUsers = userRepository.findByField("active", true);
        assertEquals(2, activeUsers.size());
        assertTrue(activeUsers.stream().allMatch(User::getActive));
        
        List<User> age25Users = userRepository.findByField("age", 25);
        assertEquals(2, age25Users.size());
        assertTrue(age25Users.stream().allMatch(u -> u.getAge() == 25));
        
        // Test findFirstByField
        Optional<User> firstActiveUser = userRepository.findFirstByField("active", true);
        assertTrue(firstActiveUser.isPresent());
        assertTrue(firstActiveUser.get().getActive());
        
        Optional<User> firstInactiveUser = userRepository.findFirstByField("active", false);
        assertTrue(firstInactiveUser.isPresent());
        assertFalse(firstInactiveUser.get().getActive());
        
        // Test findFirstByField - not found
        Optional<User> nonExistent = userRepository.findFirstByField("age", 999);
        assertFalse(nonExistent.isPresent());
    }
    
    @Test
    void testMongoRepositoryCustomQuery() {
        // Create test data
        User user1 = new User();
        user1.setUsername("mongo_custom1");
        user1.setEmail("mongoCustom1@example.com");
        user1.setAge(20);
        user1.setActive(true);
        
        User user2 = new User();
        user2.setUsername("mongo_custom2");
        user2.setEmail("mongoCustom2@example.com");
        user2.setAge(30);
        user2.setActive(true);
        
        User user3 = new User();
        user3.setUsername("mongo_custom3");
        user3.setEmail("mongoCustom3@example.com");
        user3.setAge(40);
        user3.setActive(false);
        
        userRepository.saveAll(Arrays.asList(user1, user2, user3));
        
        // Test custom query using repository's createQuery
        List<User> activeUsers = userRepository.createQuery()
                .where("active", true)
                .orderBy("age")
                .getResultList();
        
        assertEquals(2, activeUsers.size()); // user1 and user2 (active)
        assertEquals("mongo_custom1", activeUsers.get(0).getUsername());
        assertEquals("mongo_custom2", activeUsers.get(1).getUsername());
        
        // Test with limit
        List<User> limitedResults = userRepository.createQuery()
                .where("active", true)
                .orderBy("username")
                .limit(1)
                .getResultList();
        
        assertEquals(1, limitedResults.size());
    }
    
    @Test
    void testMongoRepositoryWithDepartments() {
        // Create user first
        User user = new User();
        user.setUsername("mongo_dept_user");
        user.setEmail("mongoDeptUser@example.com");
        User savedUser = userRepository.save(user);
        
        // Create departments
        Department dept1 = new Department();
        dept1.setName("MongoDB Repository Dept 1");
        dept1.setCode("DEPT001");
        dept1.setDescription("Description of the first department");
        
        Department dept2 = new Department();
        dept2.setName("MongoDB Repository Dept 2");
        dept2.setCode("DEPT002");
        dept2.setDescription("Description of the second department");
        
        List<Department> depts = Arrays.asList(dept1, dept2);
        List<Department> savedDepts = departmentRepository.saveAll(depts);
        
        assertEquals(2, savedDepts.size());
        savedDepts.forEach(dept -> {
            assertNotNull(dept.getId());
            assertNotNull(dept.getName());
        });
        
        // Test findByField with name
        List<Department> deptsByName = departmentRepository.findByField("name", "MongoDB Repository Dept 1");
        assertEquals(1, deptsByName.size());
        
        // Test department repository operations
        Optional<Department> foundDept = departmentRepository.findById(savedDepts.get(0).getId());
        assertTrue(foundDept.isPresent());
        assertEquals("MongoDB Repository Dept 1", foundDept.get().getName());
        
        // Test department count
        long deptCount = departmentRepository.count();
        assertEquals(2, deptCount);
        
        // Clean up departments first, then user
        departmentRepository.deleteAll(savedDepts);
        userRepository.delete(savedUser);
    }
    
    @Test
    void testMongoRepositoryFactory() {
        // Test basic factory usage
        Repository<User, String> repo1 = repositoryFactory.getRepository(User.class, String.class);
        Repository<User, String> repo2 = repositoryFactory.getRepository(User.class, String.class);
        
        assertNotNull(repo1);
        assertNotNull(repo2);
        assertEquals(User.class, repo1.getEntityClass());
        assertEquals(User.class, repo2.getEntityClass());
        
        // Test with Object ID type
        Repository<User, Object> repoObject = repositoryFactory.getRepository(User.class);
        assertNotNull(repoObject);
        assertEquals(User.class, repoObject.getEntityClass());
        
        // Test session access
        assertEquals(session, repositoryFactory.getSession());
    }
    
    @Test
    void testCustomMongoRepositoryImplementation() {
        // Test custom repository creation
        UserRepository customRepo = repositoryFactory.getCustomRepository(UserRepository.class);
        
        assertNotNull(customRepo);
        assertEquals(User.class, customRepo.getEntityClass());
        
        // Test custom method
        User user1 = new User();
        user1.setUsername("mongo_custom_user");
        user1.setEmail("mongoCustom@example.com");
        user1.setAge(30);
        user1.setActive(true);
        
        User user2 = new User();
        user2.setUsername("another_user");
        user2.setEmail("another@example.com");
        user2.setAge(25);
        user2.setActive(false);
        
        userRepository.saveAll(Arrays.asList(user1, user2));
        
        // Test custom method - find active users
        List<User> activeUsers = customRepo.findActiveUsers();
        assertEquals(1, activeUsers.size());
        assertEquals("mongo_custom_user", activeUsers.get(0).getUsername());
        
        // Test custom method - find by age range
        List<User> usersInRange = customRepo.findUsersByAgeRange(24, 31);
        assertEquals(2, usersInRange.size());
    }
    
    @Test
    void testMongoRepositoryWithObjectIds() {
        // Test MongoDB ObjectId handling
        User user = new User();
        user.setUsername("mongo_objectid_user");
        user.setEmail("mongoObjectId@example.com");
        
        User savedUser = userRepository.save(user);
        String objectIdString = savedUser.getId();
        
        // Verify ObjectId format (24-char hex string)
        assertNotNull(objectIdString);
        assertEquals(24, objectIdString.length());
        assertTrue(objectIdString.matches("[a-fA-F0-9]+"));
        
        // Test findById with ObjectId string
        Optional<User> foundUser = userRepository.findById(objectIdString);
        assertTrue(foundUser.isPresent());
        assertEquals(objectIdString, foundUser.get().getId());
        
        // Test existsById with ObjectId string
        assertTrue(userRepository.existsById(objectIdString));
        
        // Test deleteById with ObjectId string
        boolean deleted = userRepository.deleteById(objectIdString);
        assertTrue(deleted);
        assertFalse(userRepository.existsById(objectIdString));
    }
    
    /**
     * Custom MongoDB repository implementation for testing.
     */
    public static class UserRepository extends fr.nassime.helios.api.repository.BaseRepository<User, String> {
        
        public UserRepository(fr.nassime.helios.api.HeliosSession session) {
            super(session, User.class);
        }
        
        /**
         * Custom method to find all active users.
         */
        public List<User> findActiveUsers() {
            return createQuery()
                    .where("active", true)
                    .getResultList();
        }
        
        /**
         * Custom method to find users by age range.
         */
        public List<User> findUsersByAgeRange(int minAge, int maxAge) {
            // Note: This implementation is simplified. In a real MongoDB query,
            // you would use range operators like $gte and $lte
            List<User> allUsers = findAll();
            return allUsers.stream()
                    .filter(user -> user.getAge() >= minAge && user.getAge() <= maxAge)
                    .toList();
        }
        
        /**
         * Custom method to find users by username prefix.
         */
        public List<User> findByUsernameStartingWith(String prefix) {
            // This would typically use MongoDB regex query
            List<User> allUsers = findAll();
            return allUsers.stream()
                    .filter(user -> user.getUsername().startsWith(prefix))
                    .toList();
        }
    }
}