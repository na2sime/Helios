package fr.nassime.helios.postgres.relation;

import fr.nassime.helios.api.exception.HeliosException;
import fr.nassime.helios.postgres.AbstractPostgreSQLTest;
import fr.nassime.helios.postgres.entities.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for RelationLoader.
 */
class RelationLoaderTest extends AbstractPostgreSQLTest {
    
    @Test
    void testLoadManyToOneRelation() {
        // Create user and post
        User author = new User("author", "author@example.com");
        author = session.save(author);
        
        Post post = new Post("Test Post", "Content", author);
        post = session.save(post);
        
        // Find post without relations loaded
        Post foundPost = session.findById(Post.class, post.getId()).orElseThrow();
        assertNull(foundPost.getAuthor()); // Lazy loading, should be null initially
        
        // Load the author relation
        session.loadRelation(foundPost, "author");
        
        // Verify author is loaded
        assertNotNull(foundPost.getAuthor());
        assertEquals(author.getId(), foundPost.getAuthor().getId());
        assertEquals("author", foundPost.getAuthor().getUsername());
        assertEquals("author@example.com", foundPost.getAuthor().getEmail());
    }
    
    @Test
    void testLoadOneToManyRelation() {
        // Create user and multiple posts
        User author = new User("blogger", "blogger@example.com");
        author = session.save(author);
        
        Post post1 = new Post("Post 1", "Content 1", author);
        Post post2 = new Post("Post 2", "Content 2", author);
        Post post3 = new Post("Post 3", "Content 3", author);
        
        session.save(post1);
        session.save(post2);
        session.save(post3);
        
        // Find user without relations loaded
        User foundUser = session.findById(User.class, author.getId()).orElseThrow();
        assertNull(foundUser.getPosts()); // Lazy loading, should be null initially
        
        // Load the posts relation
        session.loadRelation(foundUser, "posts");
        
        // Verify posts are loaded
        assertNotNull(foundUser.getPosts());
        assertEquals(3, foundUser.getPosts().size());
        
        List<String> titles = foundUser.getPosts().stream()
                .map(Post::getTitle)
                .sorted()
                .toList();
        assertEquals(List.of("Post 1", "Post 2", "Post 3"), titles);
    }
    
    @Test
    void testLoadOneToOneRelation() {
        // Create user and profile
        User user = new User("profile_user", "profile@example.com");
        user = session.save(user);
        
        UserProfile profile = new UserProfile("I'm a software developer", user);
        profile.setWebsite("https://example.com");
        profile.setLocation("Paris, France");
        profile = session.save(profile);
        
        // Find user without relations loaded
        User foundUser = session.findById(User.class, user.getId()).orElseThrow();
        assertNull(foundUser.getProfile()); // Lazy loading, should be null initially
        
        // Load the profile relation
        session.loadRelation(foundUser, "profile");
        
        // Verify profile is loaded
        assertNotNull(foundUser.getProfile());
        assertEquals(profile.getId(), foundUser.getProfile().getId());
        assertEquals("I'm a software developer", foundUser.getProfile().getBio());
        assertEquals("https://example.com", foundUser.getProfile().getWebsite());
        assertEquals("Paris, France", foundUser.getProfile().getLocation());
    }
    
    @Test
    void testLoadOneToOneRelationReverse() {
        // Create user and profile
        User user = new User("reverse_user", "reverse@example.com");
        user = session.save(user);
        
        UserProfile profile = new UserProfile("Reverse relation test", user);
        profile = session.save(profile);
        
        // Find profile without relations loaded
        UserProfile foundProfile = session.findById(UserProfile.class, profile.getId()).orElseThrow();
        assertNull(foundProfile.getUser()); // Lazy loading, should be null initially
        
        // Load the user relation
        session.loadRelation(foundProfile, "user");
        
        // Verify user is loaded
        assertNotNull(foundProfile.getUser());
        assertEquals(user.getId(), foundProfile.getUser().getId());
        assertEquals("reverse_user", foundProfile.getUser().getUsername());
    }
    
    @Test
    void testLoadManyToOneWithNullForeignKey() {
        // Create post without author
        Post post = new Post();
        post.setTitle("Orphan Post");
        post.setContent("This post has no author");
        post.setCreatedAt(LocalDateTime.now());
        post = session.save(post);
        
        // Load the author relation (should handle null gracefully)
        session.loadRelation(post, "author");
        
        // Verify author remains null
        assertNull(post.getAuthor());
    }
    
    @Test
    void testLoadOneToManyWithNoRelatedEntities() {
        // Create user with no posts
        User user = new User("lonely_user", "lonely@example.com");
        user = session.save(user);
        
        // Load the posts relation
        session.loadRelation(user, "posts");
        
        // Verify posts list is empty
        assertNotNull(user.getPosts());
        assertTrue(user.getPosts().isEmpty());
    }
    
    @Test
    void testLoadRelationWithInvalidRelationName() {
        User user = new User("test_user", "test@example.com");
        User savedUser = session.save(user);
        
        assertThrows(HeliosException.class, () -> {
            session.loadRelation(savedUser, "nonExistentRelation");
        });
    }
    
    @Test
    void testLoadNestedRelations() {
        // Create user, post, and comments
        User author = new User("nested_author", "nested@example.com");
        author = session.save(author);
        
        User commenter = new User("commenter", "commenter@example.com");
        commenter = session.save(commenter);
        
        Post post = new Post("Nested Post", "Content with comments", author);
        post = session.save(post);
        
        Comment comment1 = new Comment("Great post!", post, commenter);
        Comment comment2 = new Comment("I agree!", post, author);
        session.save(comment1);
        session.save(comment2);
        
        // Load post with its author
        Post foundPost = session.findById(Post.class, post.getId()).orElseThrow();
        session.loadRelation(foundPost, "author");
        session.loadRelation(foundPost, "comments");
        
        // Verify nested loading
        assertNotNull(foundPost.getAuthor());
        assertEquals("nested_author", foundPost.getAuthor().getUsername());
        
        assertNotNull(foundPost.getComments());
        assertEquals(2, foundPost.getComments().size());
        
        // Load authors of comments
        for (Comment comment : foundPost.getComments()) {
            session.loadRelation(comment, "author");
            assertNotNull(comment.getAuthor());
        }
    }
    
    @Test
    void testLoadManyToManyRelationWarning() {
        // Create entities for ManyToMany test (even though not fully implemented)
        User user = new User("mm_user", "mm@example.com");
        user = session.save(user);
        
        Post post = new Post("Tagged Post", "Content with tags", user);
        post = session.save(post);
        
        Tag tag = new Tag("java");
        tag.setDescription("Java programming language");
        session.save(tag);
        
        // This should log a warning and set empty list
        session.loadRelation(post, "tags");
        
        // Verify empty list is set (as per current implementation)
        assertNotNull(post.getTags());
        assertTrue(post.getTags().isEmpty());
    }
    
    @Test
    void testLoadRelationWithNullEntityId() {
        // Create user without saving (no ID)
        User user = new User("no_id", "noid@example.com");
        
        // Loading relations on unsaved entity should handle gracefully
        session.loadRelation(user, "posts");
        
        // Should not throw exception, posts should remain null or empty
        // (depends on implementation details)
    }
}