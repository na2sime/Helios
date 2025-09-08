package fr.nassime.helios.api.hybrid;

import fr.nassime.helios.api.annotations.StorageType;
import fr.nassime.helios.api.annotations.enums.FetchType;
import lombok.Getter;
import lombok.Builder;

import java.lang.reflect.Field;

/**
 * Metadata for individual fields in hybrid entities.
 */
@Getter
@Builder
public class HybridFieldMetadata {
    
    private final Field javaField;
    private final String fieldName;
    private final String storageName;
    private final StorageType storageType;
    private final FetchType fetchType;
    private final boolean sync;
    private final boolean required;
    private final Class<?> fieldType;
    
    /**
     * Get the effective storage name (uses fieldName if storageName is not specified).
     */
    public String getEffectiveStorageName() {
        return storageName != null && !storageName.isEmpty() ? storageName : fieldName;
    }
    
    /**
     * Check if this field should be lazily loaded.
     */
    public boolean isLazy() {
        return fetchType == FetchType.LAZY;
    }
    
    /**
     * Check if this field participates in synchronization.
     */
    public boolean isSyncEnabled() {
        return sync;
    }
}