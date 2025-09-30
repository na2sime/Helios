package fr.nassime.helios.sql.mapping;

import fr.nassime.helios.api.annotations.*;
import fr.nassime.helios.api.annotations.enums.PersistenceType;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for EntityMapper functionality in SQL module.
 */
class EntityMapperTest {

    @Test
    void shouldExtractEntityMetadata() {
        EntityMetadata metadata = EntityMapper.getMetadata(TestUser.class);

        assertNotNull(metadata);
        assertEquals("users", metadata.getTableName());
        assertNull(metadata.getSchema()); // Schema support removed
        assertNotNull(metadata.getIdField());
        assertEquals("id", metadata.getIdField().getName());
        assertEquals("id", metadata.getIdColumnName());
        assertTrue(metadata.isIdGenerated());
    }
    
    @Test
    void shouldMapColumnsCorrectly() {
        EntityMetadata metadata = EntityMapper.getMetadata(TestUser.class);
        Map<String, ColumnMetadata> columns = metadata.getColumns();
        
        assertNotNull(columns);
        assertTrue(columns.containsKey("id"));
        assertTrue(columns.containsKey("name"));
        assertTrue(columns.containsKey("email"));
        
        ColumnMetadata nameColumn = columns.get("name");
        assertEquals("user_name", nameColumn.getColumnName());
        assertEquals(String.class, nameColumn.getFieldType());
        
        ColumnMetadata emailColumn = columns.get("email");
        assertEquals("email", emailColumn.getColumnName());
        assertEquals(String.class, emailColumn.getFieldType());
    }
    
    @Test
    void shouldIdentifyInsertableColumns() {
        EntityMetadata metadata = EntityMapper.getMetadata(TestUser.class);
        var insertableColumns = metadata.getInsertableColumns();
        
        // Should include name and email, but not id (auto-generated)
        assertEquals(2, insertableColumns.size());
        assertTrue(insertableColumns.stream().anyMatch(c -> c.getFieldName().equals("name")));
        assertTrue(insertableColumns.stream().anyMatch(c -> c.getFieldName().equals("email")));
        assertFalse(insertableColumns.stream().anyMatch(c -> c.getFieldName().equals("id")));
    }
    
    @Test
    void shouldIdentifyUpdatableColumns() {
        EntityMetadata metadata = EntityMapper.getMetadata(TestUser.class);
        var updatableColumns = metadata.getUpdatableColumns();
        
        // Should include name and email, but not id
        assertEquals(2, updatableColumns.size());
        assertTrue(updatableColumns.stream().anyMatch(c -> c.getFieldName().equals("name")));
        assertTrue(updatableColumns.stream().anyMatch(c -> c.getFieldName().equals("email")));
        assertFalse(updatableColumns.stream().anyMatch(c -> c.getFieldName().equals("id")));
    }
    
    @Test
    void shouldCacheMetadata() {
        EntityMetadata metadata1 = EntityMapper.getMetadata(TestUser.class);
        EntityMetadata metadata2 = EntityMapper.getMetadata(TestUser.class);
        
        // Should return the same instance (cached)
        assertSame(metadata1, metadata2);
    }
    
    @Test
    void shouldHandleEntityWithoutSchema() {
        EntityMetadata metadata = EntityMapper.getMetadata(SimpleEntity.class);
        
        assertNotNull(metadata);
        assertEquals("simple_entities", metadata.getTableName());
        assertNull(metadata.getSchema());
    }
    
    // Test entities
    @Persistable(name = "users", type = PersistenceType.SQL)
    static class TestUser {
        @Id
        @GeneratedValue
        @Column(name = "id")
        private Long id;
        
        @Column(name = "user_name")
        private String name;
        
        @Column(name = "email")
        private String email;
        
        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }
    
    @Persistable(name = "simple_entities", type = PersistenceType.SQL)
    static class SimpleEntity {
        @Id
        @GeneratedValue
        private Long id;
        
        @Column(name = "value")
        private String value;
        
        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
    }
}