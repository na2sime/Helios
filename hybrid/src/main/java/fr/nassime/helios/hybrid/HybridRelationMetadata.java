package fr.nassime.helios.hybrid;

import fr.nassime.helios.api.annotations.CrossStorageStrategy;
import fr.nassime.helios.api.annotations.StorageType;
import fr.nassime.helios.api.annotations.enums.FetchType;
import lombok.Builder;
import lombok.Getter;

import java.lang.reflect.Field;

/**
 * Metadata for hybrid relations containing cross-storage relationship information.
 */
@Getter
@Builder
public class HybridRelationMetadata {
    
    private final Field relationField;
    private final String relationName;
    private final Class<?> sourceEntityClass;
    private final Class<?> targetEntityClass;
    
    // Storage configuration
    private final StorageType sourceStorage;
    private final StorageType targetStorage;
    private final boolean isCrossStorage;
    
    // Relation configuration
    private final RelationType relationType; // OneToMany, ManyToOne, etc.
    private final String mappedBy;
    private final String foreignKey;
    
    // Hybrid-specific configuration
    private final CrossStorageStrategy strategy;
    private final FetchType fetchType;
    private final boolean sync;
    private final boolean maintainIntegrity;
    private final boolean cached;
    private final int cacheTtl;
    
    // Optimization settings
    private final String partitionBy;
    private final int batchSize;
    private final boolean materialized;
    private final String queryHint;
    
    /**
     * Check if this relation spans across different storage types.
     */
    public boolean isCrossStorageRelation() {
        return isCrossStorage && sourceStorage != targetStorage;
    }
    
    /**
     * Get the effective foreign key name.
     */
    public String getEffectiveForeignKey() {
        if (foreignKey != null && !foreignKey.isEmpty()) {
            return foreignKey;
        }
        
        // Generate default foreign key based on source entity
        String sourceClassName = sourceEntityClass.getSimpleName().toLowerCase();
        return sourceClassName + "_id";
    }
    
    /**
     * Check if this relation should be loaded eagerly.
     */
    public boolean isEagerLoading() {
        return fetchType == FetchType.EAGER;
    }
    
    /**
     * Check if this relation requires special cross-storage handling.
     */
    public boolean requiresSpecialHandling() {
        return isCrossStorageRelation() || 
               strategy == CrossStorageStrategy.DUPLICATE ||
               strategy == CrossStorageStrategy.MATERIALIZED_VIEW;
    }
    
    /**
     * Get partition field for time-series or large dataset optimization.
     */
    public boolean hasPartitioning() {
        return partitionBy != null && !partitionBy.isEmpty();
    }
    
    public enum RelationType {
        ONE_TO_ONE,
        ONE_TO_MANY, 
        MANY_TO_ONE,
        MANY_TO_MANY
    }
}