package fr.nassime.helios.mariadb.integration;

import fr.nassime.helios.api.HeliosSession;
import fr.nassime.helios.api.query.QueryOperator;
import fr.nassime.helios.mariadb.AbstractMariaDBTest;
import fr.nassime.helios.mariadb.entities.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the complete Helios ORM functionality with MariaDB.
 */
class HeliosOrmIntegrationTest extends AbstractMariaDBTest {
    
    @Test
    void testCompleteUserPostCommentWorkflow() {
        // 1. Create and save users
        User author = new User("john_author", "john@example.com");
        author.setFullName("John Author");
        author.setAge(30);
        author = session.save(author);
        assertNotNull(author.getId());
        
        User commenter = new User("jane_commenter", "jane@example.com");
        commenter.setFullName("Jane Commenter");
        commenter.setAge(25);
        commenter = session.save(commenter);
        assertNotNull(commenter.getId());
        
        // 2. Create and save posts
        Post post1 = new Post("First Post", "This is the first post content", author);
        post1.setPublished(true);
        post1.setViewCount(100);
        post1 = session.save(post1);
        assertNotNull(post1.getId());
        
        Post post2 = new Post("Second Post", "This is the second post content", author);
        post2.setPublished(false);
        post2.setViewCount(50);
        post2 = session.save(post2);
        assertNotNull(post2.getId());
        
        // 3. Create and save comments
        Comment comment1 = new Comment("Great post!", post1, commenter);
        comment1 = session.save(comment1);
        assertNotNull(comment1.getId());
        
        Comment comment2 = new Comment("Thanks for sharing!", post1, author);
        comment2 = session.save(comment2);
        assertNotNull(comment2.getId());
        
        Comment comment3 = new Comment("Looking forward to more!", post2, commenter);
        comment3 = session.save(comment3);
        assertNotNull(comment3.getId());
        
        // 4. Test finding entities
        Optional<User> foundAuthor = session.findById(User.class, author.getId());
        assertTrue(foundAuthor.isPresent());
        assertEquals("john_author", foundAuthor.get().getUsername());
        
        Optional<Post> foundPost = session.findById(Post.class, post1.getId());
        assertTrue(foundPost.isPresent());
        assertEquals("First Post", foundPost.get().getTitle());
        
        // 5. Test loading relations
        session.loadRelation(foundPost.get(), "author");
        assertNotNull(foundPost.get().getAuthor());
        assertEquals("john_author", foundPost.get().getAuthor().getUsername());
        
        session.loadRelation(foundPost.get(), "comments");
        assertNotNull(foundPost.get().getComments());
        assertEquals(2, foundPost.get().getComments().size());
        
        // 6. Test loading user posts
        session.loadRelation(foundAuthor.get(), "posts");
        assertNotNull(foundAuthor.get().getPosts());
        assertEquals(2, foundAuthor.get().getPosts().size());
        
        // 7. Test queries
        List<User> activeUsers = session.createQuery(User.class)
                .where("active", true)
                .orderBy("username")
                .getResultList();
        assertEquals(2, activeUsers.size());
        
        List<Post> publishedPosts = session.createQuery(Post.class)
                .where("published", true)
                .getResultList();
        assertEquals(1, publishedPosts.size());
        assertEquals("First Post", publishedPosts.get(0).getTitle());
        
        List<Post> popularPosts = session.createQuery(Post.class)
                .where("view_count", QueryOperator.GREATER_THAN, 75)
                .getResultList();
        assertEquals(1, popularPosts.size());
        assertEquals("First Post", popularPosts.get(0).getTitle());
        
        // 8. Test complex query with relations
        List<Comment> authorComments = session.createQuery(Comment.class)
                .where("author_id", author.getId())
                .getResultList();
        assertEquals(1, authorComments.size());
        assertEquals("Thanks for sharing!", authorComments.get(0).getContent());
        
        // 9. Test updates
        foundAuthor.get().setAge(31);
        foundAuthor.get().setEmail("john.updated@example.com");
        User updatedAuthor = session.save(foundAuthor.get());
        assertEquals(31, updatedAuthor.getAge());
        assertEquals("john.updated@example.com", updatedAuthor.getEmail());
        
        // Verify update persisted
        Optional<User> reloadedAuthor = session.findById(User.class, author.getId());
        assertTrue(reloadedAuthor.isPresent());
        assertEquals(31, reloadedAuthor.get().getAge());
        assertEquals("john.updated@example.com", reloadedAuthor.get().getEmail());
        
        // 10. Test delete cascade simulation
        session.delete(post1);
        
        // Verify post is deleted
        Optional<Post> deletedPost = session.findById(Post.class, post1.getId());
        assertFalse(deletedPost.isPresent());
        
        // Comments should still exist in this test (cascade would be handled by DB)
        Optional<Comment> comment = session.findById(Comment.class, comment1.getId());
        assertTrue(comment.isPresent());
    }
    
    @Test
    void testUserProfileOneToOneRelation() {
        // Create user
        User user = new User("profile_user", "profile@example.com");
        user.setFullName("Profile User");
        user = session.save(user);
        
        // Create profile
        UserProfile profile = new UserProfile("Software developer", user);
        profile.setWebsite("https://example.com");
        profile.setLocation("Paris, France");
        profile.setAvatarUrl("https://avatar.com/profile.jpg");
        profile = session.save(profile);
        
        // Test loading profile from user
        User foundUser = session.findById(User.class, user.getId()).orElseThrow();
        session.loadRelation(foundUser, "profile");
        
        assertNotNull(foundUser.getProfile());
        assertEquals("Software developer", foundUser.getProfile().getBio());
        assertEquals("https://example.com", foundUser.getProfile().getWebsite());
        assertEquals("Paris, France", foundUser.getProfile().getLocation());
        
        // Test loading user from profile
        UserProfile foundProfile = session.findById(UserProfile.class, profile.getId()).orElseThrow();
        session.loadRelation(foundProfile, "user");
        
        assertNotNull(foundProfile.getUser());
        assertEquals("profile_user", foundProfile.getUser().getUsername());
        assertEquals("profile@example.com", foundProfile.getUser().getEmail());
    }
    
    @Test
    void testTransactionRollbackScenario() {
        // Create initial data
        User user = new User("tx_user", "tx@example.com");
        user = session.save(user);
        Long userId = user.getId();
        
        // Test transaction rollback
        try {
            session.executeInTransaction((HeliosSession s) -> {
                // Update user
                User txUser = s.findById(User.class, userId).orElseThrow();
                txUser.setEmail("updated@example.com");
                s.save(txUser);
                
                // Create post
                Post post = new Post("Transaction Post", "This should be rolled back", txUser);
                s.save(post);
                
                // Force exception to trigger rollback
                throw new RuntimeException("Simulated error");
            });
        } catch (RuntimeException e) {
            assertEquals("Simulated error", e.getMessage());
        }
        
        // Verify rollback - user should have original email, no post should exist
        User foundUser = session.findById(User.class, userId).orElseThrow();
        assertEquals("tx@example.com", foundUser.getEmail());
        
        List<Post> posts = session.findAll(Post.class);
        assertTrue(posts.isEmpty());
    }
    
    @Test
    void testBulkOperationsWithQueries() {
        // Create test data
        for (int i = 1; i <= 10; i++) {
            User user = new User(String.format("user%02d", i), "user" + i + "@example.com");
            user.setAge(20 + i);
            user.setActive(i % 2 == 0); // Every other user is inactive
            session.save(user);
        }
        
        // Test bulk queries
        List<User> allUsers = session.findAll(User.class);
        assertEquals(10, allUsers.size());
        
        List<User> activeUsers = session.createQuery(User.class)
                .where("active", true)
                .getResultList();
        assertEquals(5, activeUsers.size());
        
        List<User> adults = session.createQuery(User.class)
                .where("age", QueryOperator.GREATER_THAN_OR_EQUAL, 25)
                .orderBy("age")
                .getResultList();
        assertEquals(6, adults.size());
        assertEquals(25, adults.get(0).getAge());
        assertEquals(30, adults.get(5).getAge());
        
        // Test pagination
        List<User> page1 = session.createQuery(User.class)
                .orderBy("username")
                .limit(3)
                .getResultList();
        assertEquals(3, page1.size());
        assertEquals("user01", page1.get(0).getUsername());
        
        List<User> page2 = session.createQuery(User.class)
                .orderBy("username")
                .limit(3)
                .offset(3)
                .getResultList();
        assertEquals(3, page2.size());
        assertEquals("user04", page2.get(0).getUsername());
        
        // Test count
        long totalCount = session.createQuery(User.class).count();
        assertEquals(10, totalCount);
        
        long activeCount = session.createQuery(User.class)
                .where("active", true)
                .count();
        assertEquals(5, activeCount);
    }
    
    @Test
    void testNativeQueryExecution() {
        // Create test data
        User user1 = new User("native1", "native1@example.com");
        user1.setAge(25);
        session.save(user1);
        
        User user2 = new User("native2", "native2@example.com");
        user2.setAge(35);
        session.save(user2);
        
        // Test native query
        List<User> results = session.executeNativeQuery(
            "SELECT * FROM users WHERE age > ? ORDER BY age DESC",
            User.class,
            30
        );
        
        assertEquals(1, results.size());
        assertEquals("native2", results.get(0).getUsername());
        assertEquals(35, results.get(0).getAge());
        
        // Test native update
        int updatedRows = session.executeUpdate(
            "UPDATE users SET full_name = ? WHERE username = ?",
            "Updated Name",
            "native1"
        );
        
        assertEquals(1, updatedRows);
        
        // Verify update
        User updatedUser = session.findById(User.class, user1.getId()).orElseThrow();
        assertEquals("Updated Name", updatedUser.getFullName());
    }
    
    @Test
    void testComplexRelationshipLoading() {
        // Create complex data structure
        User author = new User("complex_author", "complex@example.com");
        author = session.save(author);
        
        User commenter1 = new User("commenter1", "c1@example.com");
        commenter1 = session.save(commenter1);
        
        User commenter2 = new User("commenter2", "c2@example.com");
        commenter2 = session.save(commenter2);
        
        // Create posts
        Post post1 = new Post("Complex Post 1", "Content 1", author);
        post1 = session.save(post1);
        
        Post post2 = new Post("Complex Post 2", "Content 2", author);
        post2 = session.save(post2);
        
        // Create comments
        Comment c1 = new Comment("Comment 1 on Post 1", post1, commenter1);
        Comment c2 = new Comment("Comment 2 on Post 1", post1, commenter2);
        Comment c3 = new Comment("Comment 1 on Post 2", post2, commenter1);
        
        session.save(c1);
        session.save(c2);
        session.save(c3);
        
        // Load complete structure
        User loadedAuthor = session.findById(User.class, author.getId()).orElseThrow();
        session.loadRelation(loadedAuthor, "posts");
        
        assertNotNull(loadedAuthor.getPosts());
        assertEquals(2, loadedAuthor.getPosts().size());
        
        // Load comments for each post
        for (Post post : loadedAuthor.getPosts()) {
            session.loadRelation(post, "comments");
            assertNotNull(post.getComments());
            
            // Load authors of comments
            for (Comment comment : post.getComments()) {
                session.loadRelation(comment, "author");
                assertNotNull(comment.getAuthor());
            }
        }
        
        // Verify complete structure
        Post firstPost = loadedAuthor.getPosts().stream()
                .filter(p -> "Complex Post 1".equals(p.getTitle()))
                .findFirst()
                .orElseThrow();
        
        assertEquals(2, firstPost.getComments().size());
        assertTrue(firstPost.getComments().stream()
                .anyMatch(c -> "commenter1".equals(c.getAuthor().getUsername())));
        assertTrue(firstPost.getComments().stream()
                .anyMatch(c -> "commenter2".equals(c.getAuthor().getUsername())));
    }
}