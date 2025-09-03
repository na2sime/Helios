package fr.nassime.helios.api.repository;

import fr.nassime.helios.api.HeliosSession;
import fr.nassime.helios.api.query.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the Repository implementation.
 */
class RepositoryTest {
    
    @Mock
    private HeliosSession session;
    
    @Mock
    private Query<TestEntity> query;
    
    private Repository<TestEntity, Long> repository;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        repository = new BaseRepository<>(session, TestEntity.class);
    }
    
    @Test
    void testFindById() {
        TestEntity entity = new TestEntity(1L, "Test");
        when(session.findById(TestEntity.class, 1L)).thenReturn(Optional.of(entity));
        
        Optional<TestEntity> result = repository.findById(1L);
        
        assertTrue(result.isPresent());
        assertEquals(entity, result.get());
        verify(session).findById(TestEntity.class, 1L);
    }
    
    @Test
    void testFindByIdNotFound() {
        when(session.findById(TestEntity.class, 999L)).thenReturn(Optional.empty());
        
        Optional<TestEntity> result = repository.findById(999L);
        
        assertFalse(result.isPresent());
        verify(session).findById(TestEntity.class, 999L);
    }
    
    @Test
    void testFindAll() {
        List<TestEntity> entities = Arrays.asList(
            new TestEntity(1L, "Test1"),
            new TestEntity(2L, "Test2")
        );
        when(session.findAll(TestEntity.class)).thenReturn(entities);
        
        List<TestEntity> result = repository.findAll();
        
        assertEquals(2, result.size());
        assertEquals(entities, result);
        verify(session).findAll(TestEntity.class);
    }
    
    @Test
    void testSave() {
        TestEntity entity = new TestEntity(1L, "Test");
        when(session.save(entity)).thenReturn(entity);
        
        TestEntity result = repository.save(entity);
        
        assertEquals(entity, result);
        verify(session).save(entity);
    }
    
    @Test
    void testSaveAll() {
        List<TestEntity> entities = Arrays.asList(
            new TestEntity(1L, "Test1"),
            new TestEntity(2L, "Test2")
        );
        when(session.save(any(TestEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        List<TestEntity> result = repository.saveAll(entities);
        
        assertEquals(2, result.size());
        verify(session, times(2)).save(any(TestEntity.class));
    }
    
    @Test
    void testDeleteById() {
        TestEntity entity = new TestEntity(1L, "Test");
        when(session.findById(TestEntity.class, 1L)).thenReturn(Optional.of(entity));
        when(session.delete(entity)).thenReturn(true);
        
        boolean result = repository.deleteById(1L);
        
        assertTrue(result);
        verify(session).findById(TestEntity.class, 1L);
        verify(session).delete(entity);
    }
    
    @Test
    void testDeleteByIdNotFound() {
        when(session.findById(TestEntity.class, 999L)).thenReturn(Optional.empty());
        
        boolean result = repository.deleteById(999L);
        
        assertFalse(result);
        verify(session).findById(TestEntity.class, 999L);
        verify(session, never()).delete(any());
    }
    
    @Test
    void testDelete() {
        TestEntity entity = new TestEntity(1L, "Test");
        when(session.delete(entity)).thenReturn(true);
        
        boolean result = repository.delete(entity);
        
        assertTrue(result);
        verify(session).delete(entity);
    }
    
    @Test
    void testDeleteAll() {
        List<TestEntity> entities = Arrays.asList(
            new TestEntity(1L, "Test1"),
            new TestEntity(2L, "Test2")
        );
        when(session.delete(any(TestEntity.class))).thenReturn(true);
        
        repository.deleteAll(entities);
        
        verify(session, times(2)).delete(any(TestEntity.class));
    }
    
    @Test
    void testDeleteAllEntities() {
        List<TestEntity> entities = Arrays.asList(
            new TestEntity(1L, "Test1"),
            new TestEntity(2L, "Test2")
        );
        when(session.findAll(TestEntity.class)).thenReturn(entities);
        when(session.delete(any(TestEntity.class))).thenReturn(true);
        
        repository.deleteAll();
        
        verify(session).findAll(TestEntity.class);
        verify(session, times(2)).delete(any(TestEntity.class));
    }
    
    @Test
    void testExistsById() {
        TestEntity entity = new TestEntity(1L, "Test");
        when(session.findById(TestEntity.class, 1L)).thenReturn(Optional.of(entity));
        
        boolean result = repository.existsById(1L);
        
        assertTrue(result);
        verify(session).findById(TestEntity.class, 1L);
    }
    
    @Test
    void testExistsByIdNotFound() {
        when(session.findById(TestEntity.class, 999L)).thenReturn(Optional.empty());
        
        boolean result = repository.existsById(999L);
        
        assertFalse(result);
        verify(session).findById(TestEntity.class, 999L);
    }
    
    @Test
    void testCount() {
        List<TestEntity> entities = Arrays.asList(
            new TestEntity(1L, "Test1"),
            new TestEntity(2L, "Test2")
        );
        when(session.findAll(TestEntity.class)).thenReturn(entities);
        
        long result = repository.count();
        
        assertEquals(2, result);
        verify(session).findAll(TestEntity.class);
    }
    
    @Test
    void testCreateQuery() {
        when(session.createQuery(TestEntity.class)).thenReturn(query);
        
        Query<TestEntity> result = repository.createQuery();
        
        assertEquals(query, result);
        verify(session).createQuery(TestEntity.class);
    }
    
    @Test
    void testFindByField() {
        List<TestEntity> entities = Arrays.asList(new TestEntity(1L, "Test"));
        when(session.createQuery(TestEntity.class)).thenReturn(query);
        when(query.where("name", "Test")).thenReturn(query);
        when(query.getResultList()).thenReturn(entities);
        
        List<TestEntity> result = repository.findByField("name", "Test");
        
        assertEquals(entities, result);
        verify(session).createQuery(TestEntity.class);
        verify(query).where("name", "Test");
        verify(query).getResultList();
    }
    
    @Test
    void testFindFirstByField() {
        List<TestEntity> entities = Arrays.asList(new TestEntity(1L, "Test"));
        when(session.createQuery(TestEntity.class)).thenReturn(query);
        when(query.where("name", "Test")).thenReturn(query);
        when(query.limit(1)).thenReturn(query);
        when(query.getResultList()).thenReturn(entities);
        
        Optional<TestEntity> result = repository.findFirstByField("name", "Test");
        
        assertTrue(result.isPresent());
        assertEquals(entities.get(0), result.get());
        verify(session).createQuery(TestEntity.class);
        verify(query).where("name", "Test");
        verify(query).limit(1);
        verify(query).getResultList();
    }
    
    @Test
    void testFindFirstByFieldNotFound() {
        when(session.createQuery(TestEntity.class)).thenReturn(query);
        when(query.where("name", "NotFound")).thenReturn(query);
        when(query.limit(1)).thenReturn(query);
        when(query.getResultList()).thenReturn(Arrays.asList());
        
        Optional<TestEntity> result = repository.findFirstByField("name", "NotFound");
        
        assertFalse(result.isPresent());
        verify(session).createQuery(TestEntity.class);
        verify(query).where("name", "NotFound");
        verify(query).limit(1);
        verify(query).getResultList();
    }
    
    @Test
    void testGetEntityClass() {
        Class<TestEntity> result = repository.getEntityClass();
        
        assertEquals(TestEntity.class, result);
    }
    
    @Test
    void testRepositoryFactory() {
        RepositoryFactory factory = new RepositoryFactory(session);
        
        Repository<TestEntity, Long> repo = factory.getRepository(TestEntity.class, Long.class);
        
        assertNotNull(repo);
        assertEquals(TestEntity.class, repo.getEntityClass());
        assertEquals(session, factory.getSession());
    }
    
    @Test
    void testRepositoryFactoryWithObjectId() {
        RepositoryFactory factory = new RepositoryFactory(session);
        
        Repository<TestEntity, Object> repo = factory.getRepository(TestEntity.class);
        
        assertNotNull(repo);
        assertEquals(TestEntity.class, repo.getEntityClass());
    }
    
    // Test entity class for testing purposes
    public static class TestEntity {
        private Long id;
        private String name;
        
        public TestEntity() {}
        
        public TestEntity(Long id, String name) {
            this.id = id;
            this.name = name;
        }
        
        public Long getId() {
            return id;
        }
        
        public void setId(Long id) {
            this.id = id;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestEntity that = (TestEntity) o;
            return id != null ? id.equals(that.id) : that.id == null;
        }
        
        @Override
        public int hashCode() {
            return id != null ? id.hashCode() : 0;
        }
    }
}