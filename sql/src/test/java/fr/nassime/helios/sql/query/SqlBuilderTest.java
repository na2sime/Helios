package fr.nassime.helios.sql.query;

import fr.nassime.helios.api.annotations.Column;
import fr.nassime.helios.api.annotations.Persistable;
import fr.nassime.helios.api.annotations.GeneratedValue;
import fr.nassime.helios.api.annotations.Id;
import fr.nassime.helios.api.annotations.enums.PersistenceType;
import fr.nassime.helios.sql.mapping.EntityMapper;
import fr.nassime.helios.sql.mapping.EntityMetadata;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SqlBuilder functionality.
 */
class SqlBuilderTest {

    @Test
    void shouldBuildSelectByIdQuery() {
        EntityMetadata metadata = EntityMapper.getMetadata(TestUser.class);
        SqlBuilder.PreparedQuery query = SqlBuilder.buildSelectById(metadata);

        assertNotNull(query);
        String expectedSql = "SELECT user_name, id, email FROM users WHERE id = ?";
        assertEquals(expectedSql, query.getSql());
        assertTrue(query.getParameters().isEmpty());
    }

    @Test
    void shouldBuildSelectAllQuery() {
        EntityMetadata metadata = EntityMapper.getMetadata(TestUser.class);
        SqlBuilder.PreparedQuery query = SqlBuilder.buildSelectAll(metadata);

        assertNotNull(query);
        String expectedSql = "SELECT user_name, id, email FROM users";
        assertEquals(expectedSql, query.getSql());
        assertTrue(query.getParameters().isEmpty());
    }
    
    @Test
    void shouldBuildInsertQuery() {
        EntityMetadata metadata = EntityMapper.getMetadata(TestUser.class);
        TestUser user = new TestUser();
        user.setName("John Doe");
        user.setEmail("john@example.com");

        SqlBuilder.PreparedQuery query = SqlBuilder.buildInsert(metadata, user);

        assertNotNull(query);
        String expectedSql = "INSERT INTO users (user_name, email) VALUES (?, ?)";
        assertEquals(expectedSql, query.getSql());
        assertEquals(2, query.getParameters().size());
        assertEquals("John Doe", query.getParameters().get(0));
        assertEquals("john@example.com", query.getParameters().get(1));
    }

    @Test
    void shouldBuildUpdateQuery() {
        EntityMetadata metadata = EntityMapper.getMetadata(TestUser.class);
        TestUser user = new TestUser();
        user.setId(1L);
        user.setName("John Doe Updated");
        user.setEmail("john.updated@example.com");

        SqlBuilder.PreparedQuery query = SqlBuilder.buildUpdate(metadata, user);

        assertNotNull(query);
        String expectedSql = "UPDATE users SET user_name = ?, email = ? WHERE id = ?";
        assertEquals(expectedSql, query.getSql());
        assertEquals(3, query.getParameters().size());
        assertEquals("John Doe Updated", query.getParameters().get(0));
        assertEquals("john.updated@example.com", query.getParameters().get(1));
        assertEquals(1L, query.getParameters().get(2));
    }

    @Test
    void shouldBuildDeleteQuery() {
        EntityMetadata metadata = EntityMapper.getMetadata(TestUser.class);
        TestUser user = new TestUser();
        user.setId(1L);

        SqlBuilder.PreparedQuery query = SqlBuilder.buildDelete(metadata, user);

        assertNotNull(query);
        String expectedSql = "DELETE FROM users WHERE id = ?";
        assertEquals(expectedSql, query.getSql());
        assertEquals(1, query.getParameters().size());
        assertEquals(1L, query.getParameters().get(0));
    }

    @Test
    void shouldBuildSelectByForeignKeyQuery() {
        EntityMetadata metadata = EntityMapper.getMetadata(TestUser.class);
        SqlBuilder.PreparedQuery query = SqlBuilder.buildSelectByForeignKey(metadata, "department_id", 123L);

        assertNotNull(query);
        String expectedSql = "SELECT user_name, id, email FROM users WHERE department_id = ?";
        assertEquals(expectedSql, query.getSql());
        assertEquals(1, query.getParameters().size());
        assertEquals(123L, query.getParameters().get(0));
    }
    
    @Test
    void shouldHandleEntityWithoutSchema() {
        EntityMetadata metadata = EntityMapper.getMetadata(SimpleEntity.class);
        SqlBuilder.PreparedQuery query = SqlBuilder.buildSelectById(metadata);
        
        assertNotNull(query);
        String expectedSql = "SELECT id, value FROM simple_entities WHERE id = ?";
        assertEquals(expectedSql, query.getSql());
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