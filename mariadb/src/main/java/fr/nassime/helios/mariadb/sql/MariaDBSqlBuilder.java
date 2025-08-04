package fr.nassime.helios.mariadb.sql;

import fr.nassime.helios.sql.mapping.EntityMetadata;
import fr.nassime.helios.sql.query.SqlBuilder;

/**
 * MariaDB-specific SQL builder implementation.
 */
public class MariaDBSqlBuilder {
    
    /**
     * Build INSERT query for MariaDB.
     */
    public static SqlBuilder.PreparedQuery buildInsert(EntityMetadata metadata, Object entity) {
        return SqlBuilder.buildInsert(metadata, entity);
    }
    
    /**
     * Build UPDATE query for MariaDB.
     */
    public static SqlBuilder.PreparedQuery buildUpdate(EntityMetadata metadata, Object entity) {
        return SqlBuilder.buildUpdate(metadata, entity);
    }
    
    /**
     * Build DELETE query for MariaDB.
     */
    public static SqlBuilder.PreparedQuery buildDelete(EntityMetadata metadata, Object entity) {
        return SqlBuilder.buildDelete(metadata, entity);
    }
    
    /**
     * Build SELECT query for MariaDB.
     */
    public static SqlBuilder.PreparedQuery buildSelect(EntityMetadata metadata, Object id) {
        return SqlBuilder.buildSelectById(metadata);
    }
}