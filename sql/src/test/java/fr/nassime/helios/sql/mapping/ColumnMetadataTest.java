package fr.nassime.helios.sql.mapping;

import fr.nassime.helios.api.annotations.Column;
import fr.nassime.helios.api.annotations.Entity;
import fr.nassime.helios.api.annotations.GeneratedValue;
import fr.nassime.helios.api.annotations.Id;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ColumnMetadata functionality.
 */
class ColumnMetadataTest {

    @Test
    void shouldCreateColumnMetadataWithAnnotation() throws NoSuchFieldException {
        Field field = TestEntity.class.getDeclaredField("name");
        ColumnMetadata metadata = ColumnMetadata.builder()
                .field(field)
                .fieldName("name")
                .columnName("entity_name")
                .fieldType(String.class)
                .nullable(true)
                .updatable(true)
                .insertable(true)
                .build();
        
        assertEquals(field, metadata.getField());
        assertEquals("name", metadata.getFieldName());
        assertEquals("entity_name", metadata.getColumnName());
        assertEquals(String.class, metadata.getFieldType());
        assertTrue(metadata.isNullable());
        assertTrue(metadata.isUpdatable());
        assertTrue(metadata.isInsertable());
    }
    
    @Test
    void shouldGetAndSetFieldValues() throws NoSuchFieldException {
        Field field = TestEntity.class.getDeclaredField("name");
        ColumnMetadata metadata = ColumnMetadata.builder()
                .field(field)
                .fieldName("name")
                .columnName("entity_name")
                .fieldType(String.class)
                .build();
        
        TestEntity entity = new TestEntity();
        entity.setName("Test Name");
        
        Object value = metadata.getValue(entity);
        assertEquals("Test Name", value);
        
        metadata.setValue(entity, "New Name");
        assertEquals("New Name", entity.getName());
    }
    
    @Test
    void shouldHandleNullValues() throws NoSuchFieldException {
        Field field = TestEntity.class.getDeclaredField("name");
        ColumnMetadata metadata = ColumnMetadata.builder()
                .field(field)
                .fieldName("name")
                .columnName("entity_name")
                .fieldType(String.class)
                .build();
        
        TestEntity entity = new TestEntity();
        
        Object value = metadata.getValue(entity);
        assertNull(value);
        
        metadata.setValue(entity, null);
        assertNull(entity.getName());
    }
    
    @Test
    void shouldHandleIdField() throws NoSuchFieldException {
        Field field = TestEntity.class.getDeclaredField("id");
        ColumnMetadata metadata = ColumnMetadata.builder()
                .field(field)
                .fieldName("id")
                .columnName("id")
                .fieldType(Long.class)
                .id(true)
                .insertable(false)
                .updatable(false)
                .build();
        
        assertTrue(metadata.isId());
        assertFalse(metadata.isInsertable());
        assertFalse(metadata.isUpdatable());
    }
    
    // Test entity
    @Entity
    static class TestEntity {
        @Id
        @GeneratedValue
        private Long id;
        
        @Column(name = "entity_name")
        private String name;
        
        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
}