package fr.nassime.helios.api.id;

import java.util.UUID;

/**
 * Standard implementations of IdConverter for common ID types.
 * These converters handle the most frequently used ID patterns across different storage backends.
 */
public final class StandardIdConverters {
    
    private StandardIdConverters() {}
    
    /**
     * Identity converter - no conversion needed.
     * Used when Java and storage formats are identical.
     */
    public static class IdentityConverter<T> implements IdConverter<T, T> {
        private final Class<T> type;
        
        public IdentityConverter(Class<T> type) {
            this.type = type;
        }
        
        @Override
        public T toStorageFormat(T javaId) {
            return javaId;
        }
        
        @Override
        public T fromStorageFormat(T storageId) {
            return storageId;
        }
        
        @Override
        public Class<T> getJavaType() {
            return type;
        }
        
        @Override
        public Class<T> getStorageType() {
            return type;
        }
    }
    
    /**
     * Converter for Long IDs (common in SQL databases with auto-increment).
     */
    public static class LongIdConverter implements IdConverter<Long, Long> {
        @Override
        public Long toStorageFormat(Long javaId) {
            return javaId;
        }
        
        @Override
        public Long fromStorageFormat(Long storageId) {
            return storageId;
        }
        
        @Override
        public Class<Long> getJavaType() {
            return Long.class;
        }
        
        @Override
        public Class<Long> getStorageType() {
            return Long.class;
        }
        
        @Override
        public boolean isValidJavaId(Long javaId) {
            return javaId != null && javaId > 0;
        }
        
        @Override
        public boolean isValidStorageId(Long storageId) {
            return storageId != null && storageId > 0;
        }
    }
    
    /**
     * Converter for String IDs (common in NoSQL databases).
     */
    public static class StringIdConverter implements IdConverter<String, String> {
        @Override
        public String toStorageFormat(String javaId) {
            return javaId;
        }
        
        @Override
        public String fromStorageFormat(String storageId) {
            return storageId;
        }
        
        @Override
        public Class<String> getJavaType() {
            return String.class;
        }
        
        @Override
        public Class<String> getStorageType() {
            return String.class;
        }
        
        @Override
        public boolean isValidJavaId(String javaId) {
            return javaId != null && !javaId.trim().isEmpty();
        }
        
        @Override
        public boolean isValidStorageId(String storageId) {
            return storageId != null && !storageId.trim().isEmpty();
        }
        
        @Override
        public String generateId() {
            return UUID.randomUUID().toString();
        }
    }
    
    /**
     * Converter for UUID IDs.
     */
    public static class UUIDConverter implements IdConverter<UUID, String> {
        @Override
        public String toStorageFormat(UUID javaId) {
            return javaId != null ? javaId.toString() : null;
        }
        
        @Override
        public UUID fromStorageFormat(String storageId) {
            try {
                return storageId != null ? UUID.fromString(storageId) : null;
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid UUID format: " + storageId, e);
            }
        }
        
        @Override
        public Class<UUID> getJavaType() {
            return UUID.class;
        }
        
        @Override
        public Class<String> getStorageType() {
            return String.class;
        }
        
        @Override
        public boolean isValidJavaId(UUID javaId) {
            return javaId != null;
        }
        
        @Override
        public boolean isValidStorageId(String storageId) {
            if (storageId == null || storageId.trim().isEmpty()) {
                return false;
            }
            try {
                UUID.fromString(storageId);
                return true;
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
        
        @Override
        public UUID generateId() {
            return UUID.randomUUID();
        }
    }
    
    /**
     * MongoDB ObjectId converter (String ↔ ObjectId).
     * Handles 24-character hexadecimal ObjectId strings.
     */
    public static class MongoObjectIdConverter implements IdConverter<String, Object> {
        private static final String OBJECT_ID_REGEX = "[a-fA-F0-9]{24}";
        
        @Override
        public Object toStorageFormat(String javaId) {
            if (javaId == null) return null;
            
            if (!isValidJavaId(javaId)) {
                throw new IllegalArgumentException("Invalid ObjectId format: " + javaId);
            }
            
            // In real implementation, this would create a MongoDB ObjectId
            // For now, we'll use reflection to avoid direct MongoDB dependency in API
            try {
                Class<?> objectIdClass = Class.forName("org.bson.types.ObjectId");
                return objectIdClass.getConstructor(String.class).newInstance(javaId);
            } catch (Exception e) {
                // Fallback: return as string if ObjectId class not available
                return javaId;
            }
        }
        
        @Override
        public String fromStorageFormat(Object storageId) {
            if (storageId == null) return null;
            
            // Handle MongoDB ObjectId or String
            if (storageId.getClass().getSimpleName().equals("ObjectId")) {
                return storageId.toString();
            } else if (storageId instanceof String) {
                String stringId = (String) storageId;
                if (!isValidJavaId(stringId)) {
                    throw new IllegalArgumentException("Invalid ObjectId string format: " + stringId);
                }
                return stringId;
            } else {
                throw new IllegalArgumentException("Unsupported storage ID type: " + storageId.getClass());
            }
        }
        
        @Override
        public Class<String> getJavaType() {
            return String.class;
        }
        
        @Override
        public Class<Object> getStorageType() {
            return Object.class; // Could be ObjectId or String
        }
        
        @Override
        public boolean isValidJavaId(String javaId) {
            return javaId != null && javaId.matches(OBJECT_ID_REGEX);
        }
        
        @Override
        public boolean isValidStorageId(Object storageId) {
            if (storageId == null) return false;
            
            if (storageId.getClass().getSimpleName().equals("ObjectId")) {
                return true;
            } else if (storageId instanceof String) {
                return isValidJavaId((String) storageId);
            }
            return false;
        }
        
        @Override
        public String generateId() {
            try {
                Class<?> objectIdClass = Class.forName("org.bson.types.ObjectId");
                Object objectId = objectIdClass.getDeclaredConstructor().newInstance();
                return objectId.toString();
            } catch (Exception e) {
                // Fallback: generate a pseudo-ObjectId
                return String.format("%024x", System.nanoTime());
            }
        }
    }
    
    // Factory methods for convenience
    
    public static LongIdConverter longId() {
        return new LongIdConverter();
    }
    
    public static StringIdConverter stringId() {
        return new StringIdConverter();
    }
    
    public static UUIDConverter uuid() {
        return new UUIDConverter();
    }
    
    public static MongoObjectIdConverter mongoObjectId() {
        return new MongoObjectIdConverter();
    }
    
    public static <T> IdentityConverter<T> identity(Class<T> type) {
        return new IdentityConverter<>(type);
    }
}