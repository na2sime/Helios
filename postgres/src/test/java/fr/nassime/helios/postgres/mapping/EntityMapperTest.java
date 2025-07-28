package fr.nassime.helios.postgres.mapping;

import fr.nassime.helios.api.annotations.enums.GenerationType;
import fr.nassime.helios.api.annotations.enums.RelationType;
import fr.nassime.helios.postgres.entities.Post;
import fr.nassime.helios.postgres.entities.User;
import fr.nassime.helios.postgres.entities.UserProfile;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EntityMapper.
 */
class EntityMapperTest {
    
    @Test
    void testGetMetadataForUser() {
        EntityMetadata metadata = EntityMapper.getMetadata(User.class);
        
        // Basic entity info
        assertEquals(User.class, metadata.getEntityClass());
        assertEquals("users", metadata.getTableName());
        assertNull(metadata.getSchema());
        
        // ID field
        assertEquals("id", metadata.getIdField().getName());
        assertEquals("id", metadata.getIdColumnName());
        assertTrue(metadata.isIdGenerated());
        assertEquals(GenerationType.IDENTITY, metadata.getGenerationType());
        
        // Columns
        assertTrue(metadata.getColumns().containsKey("id"));
        assertTrue(metadata.getColumns().containsKey("username"));
        assertTrue(metadata.getColumns().containsKey("email"));
        assertTrue(metadata.getColumns().containsKey("fullName"));
        assertTrue(metadata.getColumns().containsKey("age"));
        assertTrue(metadata.getColumns().containsKey("active"));
        assertTrue(metadata.getColumns().containsKey("createdAt"));
        
        // Column metadata
        ColumnMetadata usernameCol = metadata.getColumns().get("username");
        assertEquals("username", usernameCol.getColumnName());
        assertEquals(String.class, usernameCol.getFieldType());
        assertFalse(usernameCol.isNullable());
        assertTrue(usernameCol.isUnique());
        
        ColumnMetadata emailCol = metadata.getColumns().get("email");
        assertEquals("email", emailCol.getColumnName());
        assertFalse(emailCol.isNullable());
        assertFalse(emailCol.isUnique());
        
        ColumnMetadata fullNameCol = metadata.getColumns().get("fullName");
        assertEquals("full_name", fullNameCol.getColumnName());
        assertTrue(fullNameCol.isNullable());
        
        // Relations
        assertEquals(2, metadata.getRelations().size());
        
        RelationMetadata postsRelation = metadata.getRelations().stream()
                .filter(r -> "posts".equals(r.getFieldName()))
                .findFirst()
                .orElseThrow();
        assertEquals(RelationType.ONE_TO_MANY, postsRelation.getRelationType());
        assertEquals(Post.class, postsRelation.getTargetEntity());
        assertEquals("author", postsRelation.getMappedBy());
        
        RelationMetadata profileRelation = metadata.getRelations().stream()
                .filter(r -> "profile".equals(r.getFieldName()))
                .findFirst()
                .orElseThrow();
        assertEquals(RelationType.ONE_TO_ONE, profileRelation.getRelationType());
        assertEquals(UserProfile.class, profileRelation.getTargetEntity());
        assertEquals("user", profileRelation.getMappedBy());
    }
    
    @Test
    void testGetMetadataForPost() {
        EntityMetadata metadata = EntityMapper.getMetadata(Post.class);
        
        // Basic entity info
        assertEquals(Post.class, metadata.getEntityClass());
        assertEquals("posts", metadata.getTableName());
        
        // Relations
        assertTrue(metadata.getRelations().size() >= 3);
        
        RelationMetadata authorRelation = metadata.getRelations().stream()
                .filter(r -> "author".equals(r.getFieldName()))
                .findFirst()
                .orElseThrow();
        assertEquals(RelationType.MANY_TO_ONE, authorRelation.getRelationType());
        assertEquals(User.class, authorRelation.getTargetEntity());
        assertEquals("author_id", authorRelation.getJoinColumn());
        
        RelationMetadata commentsRelation = metadata.getRelations().stream()
                .filter(r -> "comments".equals(r.getFieldName()))
                .findFirst()
                .orElseThrow();
        assertEquals(RelationType.ONE_TO_MANY, commentsRelation.getRelationType());
        assertEquals("post", commentsRelation.getMappedBy());
    }
    
    @Test
    void testGetInsertableColumns() {
        EntityMetadata metadata = EntityMapper.getMetadata(User.class);
        List<ColumnMetadata> insertableColumns = metadata.getInsertableColumns();
        
        // Should exclude ID column for IDENTITY generation
        assertFalse(insertableColumns.stream().anyMatch(c -> "id".equals(c.getColumnName())));
        
        // Should include other columns
        assertTrue(insertableColumns.stream().anyMatch(c -> "username".equals(c.getColumnName())));
        assertTrue(insertableColumns.stream().anyMatch(c -> "email".equals(c.getColumnName())));
        assertTrue(insertableColumns.stream().anyMatch(c -> "active".equals(c.getColumnName())));
    }
    
    @Test
    void testGetUpdatableColumns() {
        EntityMetadata metadata = EntityMapper.getMetadata(User.class);
        List<ColumnMetadata> updatableColumns = metadata.getUpdatableColumns();
        
        // Should exclude ID column
        assertFalse(updatableColumns.stream().anyMatch(c -> "id".equals(c.getColumnName())));
        
        // Should include other columns
        assertTrue(updatableColumns.stream().anyMatch(c -> "username".equals(c.getColumnName())));
        assertTrue(updatableColumns.stream().anyMatch(c -> "email".equals(c.getColumnName())));
    }
    
    @Test
    void testGetColumnNames() {
        EntityMetadata metadata = EntityMapper.getMetadata(User.class);
        List<String> columnNames = metadata.getColumnNames();
        
        assertTrue(columnNames.contains("id"));
        assertTrue(columnNames.contains("username"));
        assertTrue(columnNames.contains("email"));
        assertTrue(columnNames.contains("full_name"));
        assertTrue(columnNames.contains("age"));
        assertTrue(columnNames.contains("active"));
        assertTrue(columnNames.contains("created_at"));
    }
    
    @Test
    void testColumnValueGetterSetter() throws Exception {
        EntityMetadata metadata = EntityMapper.getMetadata(User.class);
        ColumnMetadata usernameCol = metadata.getColumns().get("username");
        
        User user = new User();
        
        // Test setter
        usernameCol.setValue(user, "test_user");
        assertEquals("test_user", user.getUsername());
        
        // Test getter
        Object value = usernameCol.getValue(user);
        assertEquals("test_user", value);
    }
    
    @Test
    void testRelationValueGetterSetter() throws Exception {
        EntityMetadata metadata = EntityMapper.getMetadata(User.class);
        RelationMetadata postsRelation = metadata.getRelations().stream()
                .filter(r -> "posts".equals(r.getFieldName()))
                .findFirst()
                .orElseThrow();
        
        User user = new User();
        List<Post> posts = List.of(new Post("Test", "Content", user));
        
        // Test setter
        postsRelation.setValue(user, posts);
        assertEquals(posts, user.getPosts());
        
        // Test getter
        Object value = postsRelation.getValue(user);
        assertEquals(posts, value);
    }
    
    @Test
    void testMetadataCaching() {
        // Get metadata twice
        EntityMetadata metadata1 = EntityMapper.getMetadata(User.class);
        EntityMetadata metadata2 = EntityMapper.getMetadata(User.class);
        
        // Should return the same instance (cached)
        assertSame(metadata1, metadata2);
    }
    
    @Test
    void testColumnWithNullableAnnotation() {
        EntityMetadata metadata = EntityMapper.getMetadata(User.class);
        
        // full_name should be nullable (no nullable=false specified)
        ColumnMetadata fullNameCol = metadata.getColumns().get("fullName");
        assertTrue(fullNameCol.isNullable());
        
        // username should not be nullable
        ColumnMetadata usernameCol = metadata.getColumns().get("username");
        assertFalse(usernameCol.isNullable());
        
        // email should not be nullable
        ColumnMetadata emailCol = metadata.getColumns().get("email");
        assertFalse(emailCol.isNullable());
    }
    
    @Test
    void testColumnWithUniqueAnnotation() {
        EntityMetadata metadata = EntityMapper.getMetadata(User.class);
        
        // username should be unique
        ColumnMetadata usernameCol = metadata.getColumns().get("username");
        assertTrue(usernameCol.isUnique());
        
        // email should not be unique
        ColumnMetadata emailCol = metadata.getColumns().get("email");
        assertFalse(emailCol.isUnique());
    }
    
    @Test
    void testFieldTypes() {
        EntityMetadata metadata = EntityMapper.getMetadata(User.class);
        
        assertEquals(Long.class, metadata.getColumns().get("id").getFieldType());
        assertEquals(String.class, metadata.getColumns().get("username").getFieldType());
        assertEquals(String.class, metadata.getColumns().get("email").getFieldType());
        assertEquals(String.class, metadata.getColumns().get("fullName").getFieldType());
        assertEquals(Integer.class, metadata.getColumns().get("age").getFieldType());
        assertEquals(Boolean.class, metadata.getColumns().get("active").getFieldType());
        assertEquals(LocalDateTime.class, metadata.getColumns().get("createdAt").getFieldType());
    }
}