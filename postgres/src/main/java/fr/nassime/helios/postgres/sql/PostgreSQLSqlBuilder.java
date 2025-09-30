package fr.nassime.helios.postgres.sql;

import fr.nassime.helios.sql.mapping.EntityMetadata;
import fr.nassime.helios.sql.query.SqlBuilder;

/**
 * PostgreSQL-specific SQL builder that extends the generic SQL builder
 * to add PostgreSQL-specific features like RETURNING clause.
 */
public class PostgreSQLSqlBuilder extends SqlBuilder {
    
    /**
     * Build INSERT query with PostgreSQL-specific RETURNING clause.
     */
    public static PreparedQuery buildInsert(EntityMetadata metadata, Object entity) {
        return SqlBuilder.buildInsert(metadata, entity, true); // Use RETURNING clause
    }
}