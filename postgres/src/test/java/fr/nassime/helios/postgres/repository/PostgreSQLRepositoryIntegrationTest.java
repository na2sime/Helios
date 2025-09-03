package fr.nassime.helios.postgres.repository;

import fr.nassime.helios.api.repository.Repository;
import fr.nassime.helios.api.repository.RepositoryFactory;
import fr.nassime.helios.postgres.AbstractPostgreSQLTest;
import fr.nassime.helios.postgres.entities.User;
import fr.nassime.helios.postgres.entities.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Repository pattern with PostgreSQL.
 */
class PostgreSQLRepositoryIntegrationTest extends AbstractPostgreSQLTest {
    
    private Repository<User, Long> userRepository;
    private Repository<UserProfile, Long> profileRepository;
    private RepositoryFactory repositoryFactory;
    
    @BeforeEach
    void setUpRepositories() {
        // The session is already initialized by AbstractPostgreSQLTest.setUp()
        if (session == null) {
            throw new IllegalStateException("Session should be initialized by AbstractPostgreSQLTest");
        }
        repositoryFactory = new RepositoryFactory(session);
        userRepository = repositoryFactory.getRepository(User.class, Long.class);
        profileRepository = repositoryFactory.getRepository(UserProfile.class, Long.class);
    }
    
    @Test
    void testRepositoryBasicOperations() {
        // Test save
        User user = new User("repo_user", "repo@example.com");
        user.setFullName("Repository User");
        user.setAge(30);
        
        User savedUser = userRepository.save(user);
        assertNotNull(savedUser.getId());
        assertEquals("repo_user", savedUser.getUsername());
        
        // Test findById
        Optional<User> foundUser = userRepository.findById(savedUser.getId());
        assertTrue(foundUser.isPresent());
        assertEquals(savedUser.getId(), foundUser.get().getId());
        assertEquals("repo_user", foundUser.get().getUsername());
        
        // Test existsById
        assertTrue(userRepository.existsById(savedUser.getId()));
        assertFalse(userRepository.existsById(999L));
        
        // Test update
        savedUser.setFullName("Updated Repository User");
        User updatedUser = userRepository.save(savedUser);
        assertEquals("Updated Repository User", updatedUser.getFullName());
        
        // Test delete
        boolean deleted = userRepository.delete(savedUser);
        assertTrue(deleted);
        
        // Verify deletion
        Optional<User> deletedUser = userRepository.findById(savedUser.getId());
        assertFalse(deletedUser.isPresent());
    }
    
    @Test
    void testRepositoryBatchOperations() {
        // Create multiple users
        List<User> users = Arrays.asList(
            new User("batch1", "batch1@example.com"),
            new User("batch2", "batch2@example.com"),
            new User("batch3", "batch3@example.com")
        );
        
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
        assertEquals("batch3", remainingUsers.get(0).getUsername());
        
        // Test deleteAll (all remaining)
        userRepository.deleteAll();
        
        List<User> finalUsers = userRepository.findAll();
        assertEquals(0, finalUsers.size());
    }
    
    @Test
    void testRepositoryDeleteById() {
        User user = new User("delete_by_id", "deletebyid@example.com");
        User savedUser = userRepository.save(user);
        Long userId = savedUser.getId();
        
        // Test deleteById - existing
        boolean deleted = userRepository.deleteById(userId);
        assertTrue(deleted);
        
        // Verify deletion
        assertFalse(userRepository.existsById(userId));
        
        // Test deleteById - non-existing
        boolean notDeleted = userRepository.deleteById(999L);
        assertFalse(notDeleted);
    }
    
    @Test
    void testRepositoryQueryMethods() {
        // Create test data
        User user1 = new User("query1", "query1@example.com");
        user1.setAge(25);
        user1.setActive(true);
        
        User user2 = new User("query2", "query2@example.com");
        user2.setAge(35);
        user2.setActive(false);
        
        User user3 = new User("query3", "query3@example.com");
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
    void testRepositoryCustomQuery() {
        // Create test data
        User user1 = new User("custom1", "custom1@example.com");
        user1.setAge(20);
        user1.setActive(true);
        
        User user2 = new User("custom2", "custom2@example.com");
        user2.setAge(30);
        user2.setActive(true);
        
        User user3 = new User("custom3", "custom3@example.com");
        user3.setAge(40);
        user3.setActive(false);
        
        userRepository.saveAll(Arrays.asList(user1, user2, user3));
        
        // Test custom query using repository's createQuery
        List<User> activeUsers = userRepository.createQuery()
                .where("active", true)
                .orderBy("age")
                .getResultList();
        
        assertEquals(2, activeUsers.size()); // user1 and user2 are active
        assertEquals("custom1", activeUsers.get(0).getUsername()); // age 20
        assertEquals("custom2", activeUsers.get(1).getUsername()); // age 30
        
        // Test with limit
        List<User> limitedResults = userRepository.createQuery()
                .where("active", true)
                .orderBy("username")
                .limit(2)
                .getResultList();
        
        assertEquals(2, limitedResults.size());
    }
    
    @Test
    void testRepositoryWithRelations() {
        // Create user with profile
        User user = new User("relation_user", "relation@example.com");
        User savedUser = userRepository.save(user);
        
        UserProfile profile = new UserProfile();
        profile.setBio("Test bio");
        profile.setWebsite("https://example.com");
        profile.setUserId(savedUser.getId());
        
        UserProfile savedProfile = profileRepository.save(profile);
        assertNotNull(savedProfile.getId());
        assertEquals(savedUser.getId(), savedProfile.getUserId());
        
        // Test profile repository operations
        Optional<UserProfile> foundProfile = profileRepository.findById(savedProfile.getId());
        assertTrue(foundProfile.isPresent());
        assertEquals("Test bio", foundProfile.get().getBio());
        
        // Test findByField with foreign key
        List<UserProfile> userProfiles = profileRepository.findByField("user_id", savedUser.getId());
        assertEquals(1, userProfiles.size());
        assertEquals(savedProfile.getId(), userProfiles.get(0).getId());
    }
    
    @Test
    void testRepositoryFactory() {
        // Test basic factory usage
        Repository<User, Long> repo1 = repositoryFactory.getRepository(User.class, Long.class);
        Repository<User, Long> repo2 = repositoryFactory.getRepository(User.class, Long.class);
        
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
    void testCustomRepositoryImplementation() {
        // Test custom repository creation would be done here
        // This would require a custom repository class extending BaseRepository
        UserRepository customRepo = repositoryFactory.getCustomRepository(UserRepository.class);
        
        assertNotNull(customRepo);
        assertEquals(User.class, customRepo.getEntityClass());
        
        // Test custom method
        User user = new User("custom_repo_user", "customrepo@example.com");
        user.setFullName("Custom Repository User");
        userRepository.save(user);
        
        List<User> usersWithCustom = customRepo.findByUsernameContaining("custom");
        assertEquals(1, usersWithCustom.size());
        assertEquals("custom_repo_user", usersWithCustom.get(0).getUsername());
    }
    
    /**
     * Custom repository implementation for testing.
     */
    public static class UserRepository extends fr.nassime.helios.api.repository.BaseRepository<User, Long> {
        
        public UserRepository(fr.nassime.helios.api.HeliosSession session) {
            super(session, User.class);
        }
        
        /**
         * Custom method to find users by username containing a specific string.
         */
        public List<User> findByUsernameContaining(String substring) {
            return getSession().executeNativeQuery(
                    "SELECT * FROM users WHERE username LIKE ?",
                    User.class,
                    "%" + substring + "%"
            );
        }
        
        /**
         * Custom method to find active users by age range.
         */
        public List<User> findActiveUsersByAgeRange(int minAge, int maxAge) {
            return createQuery()
                    .where("active", true)
                    .where("age", minAge) // This would need proper range support in Query
                    .getResultList();
        }
    }
}