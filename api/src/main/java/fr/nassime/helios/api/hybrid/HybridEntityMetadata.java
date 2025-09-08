package fr.nassime.helios.api.hybrid;

import fr.nassime.helios.api.annotations.StorageType;
import fr.nassime.helios.api.annotations.enums.SynchronizationStrategy;
import lombok.Getter;
import lombok.Builder;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Metadata for hybrid entities containing information about field storage distribution
 * and synchronization strategies.
 */
@Getter
@Builder
public class HybridEntityMetadata {
    
    private final Class<?> entityClass;
    private final String relationalTable;
    private final String documentCollection;
    private final SynchronizationStrategy strategy;
    private final boolean autoSync;
    private final StorageType primaryStorage;
    
    private final List<HybridFieldMetadata> relationalFields;
    private final List<HybridFieldMetadata> documentFields;
    private final List<HybridFieldMetadata> bothFields;
    
    private final Field idField;
    
    /**
     * Get all fields stored in a specific storage type.
     */
    public List<HybridFieldMetadata> getFieldsByStorage(StorageType storageType) {
        return switch (storageType) {
            case RELATIONAL -> relationalFields;
            case DOCUMENT -> documentFields;
            case BOTH -> bothFields;
        };
    }
    
    /**
     * Check if this entity has any document fields.
     */
    public boolean hasDocumentFields() {
        return !documentFields.isEmpty() || !bothFields.isEmpty();
    }
    
    /**
     * Check if this entity has any relational fields.
     */
    public boolean hasRelationalFields() {
        return !relationalFields.isEmpty() || !bothFields.isEmpty();
    }
}