package fr.nassime.helios.postgres.relation;

import fr.nassime.helios.api.exception.HeliosException;
import fr.nassime.helios.postgres.mapping.EntityMapper;
import fr.nassime.helios.postgres.mapping.EntityMetadata;
import fr.nassime.helios.postgres.mapping.RelationMetadata;
import fr.nassime.helios.postgres.mapping.ResultSetMapper;
import fr.nassime.helios.postgres.session.PostgreSQLSession;
import lombok.extern.slf4j.Slf4j;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles loading of entity relationships.
 */
@Slf4j
public class RelationLoader {
    
    private final PostgreSQLSession session;
    
    public RelationLoader(PostgreSQLSession session) {
        this.session = session;
    }
    
    /**
     * Load a specific relation for an entity.
     */
    public <T> void loadRelation(T entity, String relationName) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }
        
        EntityMetadata metadata = EntityMapper.getMetadata(entity.getClass());
        RelationMetadata relationMetadata = findRelationMetadata(metadata, relationName);
        
        if (relationMetadata == null) {
            throw new HeliosException("Relation '" + relationName + "' not found in entity " + entity.getClass().getSimpleName());
        }
        
        log.debug("Loading relation '{}' for entity {}", relationName, entity.getClass().getSimpleName());
        
        switch (relationMetadata.getRelationType()) {
            case MANY_TO_ONE -> loadManyToOneRelation(entity, metadata, relationMetadata);
            case ONE_TO_MANY -> loadOneToManyRelation(entity, metadata, relationMetadata);
            case ONE_TO_ONE -> loadOneToOneRelation(entity, metadata, relationMetadata);
            case MANY_TO_MANY -> loadManyToManyRelation(entity, metadata, relationMetadata);
        }
    }
    
    /**
     * Find relation metadata by field name.
     */
    private RelationMetadata findRelationMetadata(EntityMetadata metadata, String relationName) {
        return metadata.getRelations().stream()
                .filter(rel -> rel.getFieldName().equals(relationName))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Load ManyToOne relation (foreign key in current entity).
     */
    private <T> void loadManyToOneRelation(T entity, EntityMetadata entityMetadata, RelationMetadata relationMetadata) {
        // Get the foreign key value from the entity
        String joinColumn = relationMetadata.getJoinColumn();
        Object foreignKeyValue = getForeignKeyValue(entity, entityMetadata, joinColumn);
        
        if (foreignKeyValue == null) {
            return; // No relation to load
        }
        
        // Load the related entity by ID
        EntityMetadata targetMetadata = EntityMapper.getMetadata(relationMetadata.getTargetEntity());
        String sql = buildSelectByIdQuery(targetMetadata);
        
        session.executeWithConnection(connection -> {
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setObject(1, foreignKeyValue);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    ResultSetMapper mapper = new ResultSetMapper();
                    Object relatedEntity = mapper.mapToEntity(rs, relationMetadata.getTargetEntity());
                    relationMetadata.setValue(entity, relatedEntity);
                }
            } catch (SQLException e) {
                throw new HeliosException("Failed to load ManyToOne relation", e);
            }
            return null;
        });
    }
    
    /**
     * Load OneToMany relation (foreign key in target entities).
     */
    private <T> void loadOneToManyRelation(T entity, EntityMetadata entityMetadata, RelationMetadata relationMetadata) {
        // Get the ID of the current entity
        Object entityId = getEntityId(entity, entityMetadata);
        
        if (entityId == null) {
            return; // No ID, can't load relations
        }
        
        // Build query to find all related entities
        EntityMetadata targetMetadata = EntityMapper.getMetadata(relationMetadata.getTargetEntity());
        String mappedBy = relationMetadata.getMappedBy();
        
        // If mappedBy is specified, use it; otherwise derive the join column
        String joinColumn = mappedBy.isEmpty() 
            ? deriveJoinColumn(entityMetadata.getEntityClass()) 
            : mappedBy;
        
        String sql = buildSelectByForeignKeyQuery(targetMetadata, joinColumn);
        
        session.executeWithConnection(connection -> {
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setObject(1, entityId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    ResultSetMapper mapper = new ResultSetMapper();
                    List<?> relatedEntities = mapper.mapToList(rs, relationMetadata.getTargetEntity());
                    relationMetadata.setValue(entity, relatedEntities);
                }
            } catch (SQLException e) {
                throw new HeliosException("Failed to load OneToMany relation", e);
            }
            return null;
        });
    }
    
    /**
     * Load OneToOne relation.
     */
    private <T> void loadOneToOneRelation(T entity, EntityMetadata entityMetadata, RelationMetadata relationMetadata) {
        if (relationMetadata.getMappedBy().isEmpty()) {
            // This side owns the relationship (has foreign key)
            loadManyToOneRelation(entity, entityMetadata, relationMetadata);
        } else {
            // The other side owns the relationship
            Object entityId = getEntityId(entity, entityMetadata);
            if (entityId == null) {
                return;
            }
            
            EntityMetadata targetMetadata = EntityMapper.getMetadata(relationMetadata.getTargetEntity());
            String joinColumn = relationMetadata.getMappedBy();
            String sql = buildSelectByForeignKeyQuery(targetMetadata, joinColumn);
            
            session.executeWithConnection(connection -> {
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setObject(1, entityId);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        ResultSetMapper mapper = new ResultSetMapper();
                        Object relatedEntity = mapper.mapToEntity(rs, relationMetadata.getTargetEntity());
                        relationMetadata.setValue(entity, relatedEntity);
                    }
                } catch (SQLException e) {
                    throw new HeliosException("Failed to load OneToOne relation", e);
                }
                return null;
            });
        }
    }
    
    /**
     * Load ManyToMany relation (requires join table).
     */
    private <T> void loadManyToManyRelation(T entity, EntityMetadata entityMetadata, RelationMetadata relationMetadata) {
        Object entityId = getEntityId(entity, entityMetadata);
        if (entityId == null) {
            return;
        }
        
        // For now, log that ManyToMany is not fully implemented
        // In a full implementation, you'd need to handle join tables
        log.warn("ManyToMany relation loading not fully implemented yet for relation: {}", relationMetadata.getFieldName());
        relationMetadata.setValue(entity, new ArrayList<>());
    }
    
    /**
     * Get foreign key value from an entity.
     */
    private Object getForeignKeyValue(Object entity, EntityMetadata metadata, String joinColumn) {
        return metadata.getColumns().values().stream()
                .filter(col -> col.getColumnName().equals(joinColumn))
                .findFirst()
                .map(col -> col.getValue(entity))
                .orElse(null);
    }
    
    /**
     * Get entity ID value.
     */
    private Object getEntityId(Object entity, EntityMetadata metadata) {
        return metadata.getColumns().get(metadata.getIdField().getName()).getValue(entity);
    }
    
    /**
     * Derive join column name from entity class.
     */
    private String deriveJoinColumn(Class<?> entityClass) {
        String className = entityClass.getSimpleName().toLowerCase();
        return className + "_id";
    }
    
    /**
     * Build SELECT query for finding by ID.
     */
    private String buildSelectByIdQuery(EntityMetadata metadata) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        
        // Add columns
        List<String> columns = new ArrayList<>();
        for (String columnName : metadata.getColumnNames()) {
            columns.add(columnName);
        }
        sql.append(String.join(", ", columns));
        
        sql.append(" FROM ");
        if (metadata.getSchema() != null) {
            sql.append(metadata.getSchema()).append(".");
        }
        sql.append(metadata.getTableName());
        
        sql.append(" WHERE ").append(metadata.getIdColumnName()).append(" = ?");
        
        return sql.toString();
    }
    
    /**
     * Build SELECT query for finding by foreign key.
     */
    private String buildSelectByForeignKeyQuery(EntityMetadata metadata, String foreignKeyColumn) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        
        // Add columns
        List<String> columns = new ArrayList<>();
        for (String columnName : metadata.getColumnNames()) {
            columns.add(columnName);
        }
        sql.append(String.join(", ", columns));
        
        sql.append(" FROM ");
        if (metadata.getSchema() != null) {
            sql.append(metadata.getSchema()).append(".");
        }
        sql.append(metadata.getTableName());
        
        sql.append(" WHERE ").append(foreignKeyColumn).append(" = ?");
        
        return sql.toString();
    }
}