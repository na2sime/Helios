package fr.nassime.helios.sql.query;

import fr.nassime.helios.sql.mapping.ColumnMetadata;
import fr.nassime.helios.sql.mapping.EntityMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * Builds SQL queries for SQL databases.
 * This is a generic implementation that can be extended by specific database providers.
 */
public class SqlBuilder {
    
    /**
     * Build SELECT query for finding by ID.
     */
    public static PreparedQuery buildSelectById(EntityMetadata metadata) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        
        // Add columns
        StringJoiner columns = new StringJoiner(", ");
        for (ColumnMetadata column : metadata.getColumns().values()) {
            columns.add(column.getColumnName());
        }
        sql.append(columns);
        
        sql.append(" FROM ");
        if (metadata.getSchema() != null) {
            sql.append(metadata.getSchema()).append(".");
        }
        sql.append(metadata.getTableName());
        
        sql.append(" WHERE ").append(metadata.getIdColumnName()).append(" = ?");
        
        return new PreparedQuery(sql.toString(), List.of());
    }
    
    /**
     * Build SELECT query for finding all.
     */
    public static PreparedQuery buildSelectAll(EntityMetadata metadata) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        
        // Add columns
        StringJoiner columns = new StringJoiner(", ");
        for (ColumnMetadata column : metadata.getColumns().values()) {
            columns.add(column.getColumnName());
        }
        sql.append(columns);
        
        sql.append(" FROM ");
        if (metadata.getSchema() != null) {
            sql.append(metadata.getSchema()).append(".");
        }
        sql.append(metadata.getTableName());
        
        return new PreparedQuery(sql.toString(), List.of());
    }
    
    /**
     * Build INSERT query.
     * This method can be overridden by database-specific implementations
     * to handle database-specific features like RETURNING clause.
     */
    public static PreparedQuery buildInsert(EntityMetadata metadata, Object entity) {
        return buildInsert(metadata, entity, false);
    }
    
    /**
     * Build INSERT query with option to return generated keys.
     */
    public static PreparedQuery buildInsert(EntityMetadata metadata, Object entity, boolean useReturningClause) {
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ");
        
        if (metadata.getSchema() != null) {
            sql.append(metadata.getSchema()).append(".");
        }
        sql.append(metadata.getTableName());
        
        List<ColumnMetadata> insertableColumns = metadata.getInsertableColumns();
        
        // Column names
        sql.append(" (");
        StringJoiner columnNames = new StringJoiner(", ");
        for (ColumnMetadata column : insertableColumns) {
            columnNames.add(column.getColumnName());
        }
        sql.append(columnNames);
        sql.append(")");
        
        // Values
        sql.append(" VALUES (");
        StringJoiner placeholders = new StringJoiner(", ");
        List<Object> parameters = new ArrayList<>();
        
        for (ColumnMetadata column : insertableColumns) {
            placeholders.add("?");
            parameters.add(column.getValue(entity));
        }
        
        sql.append(placeholders);
        sql.append(")");
        
        // Add RETURNING clause if supported and needed (PostgreSQL-specific)
        if (useReturningClause && metadata.isIdGenerated()) {
            sql.append(" RETURNING ").append(metadata.getIdColumnName());
        }
        
        return new PreparedQuery(sql.toString(), parameters);
    }
    
    /**
     * Build UPDATE query.
     */
    public static PreparedQuery buildUpdate(EntityMetadata metadata, Object entity) {
        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE ");
        
        if (metadata.getSchema() != null) {
            sql.append(metadata.getSchema()).append(".");
        }
        sql.append(metadata.getTableName());
        
        List<ColumnMetadata> updatableColumns = metadata.getUpdatableColumns();
        List<Object> parameters = new ArrayList<>();
        
        // SET clause
        sql.append(" SET ");
        StringJoiner setClause = new StringJoiner(", ");
        for (ColumnMetadata column : updatableColumns) {
            setClause.add(column.getColumnName() + " = ?");
            parameters.add(column.getValue(entity));
        }
        sql.append(setClause);
        
        // WHERE clause
        sql.append(" WHERE ").append(metadata.getIdColumnName()).append(" = ?");
        ColumnMetadata idColumn = metadata.getColumns().get(metadata.getIdField().getName());
        parameters.add(idColumn.getValue(entity));
        
        return new PreparedQuery(sql.toString(), parameters);
    }
    
    /**
     * Build DELETE query.
     */
    public static PreparedQuery buildDelete(EntityMetadata metadata, Object entity) {
        StringBuilder sql = new StringBuilder();
        sql.append("DELETE FROM ");
        
        if (metadata.getSchema() != null) {
            sql.append(metadata.getSchema()).append(".");
        }
        sql.append(metadata.getTableName());
        
        sql.append(" WHERE ").append(metadata.getIdColumnName()).append(" = ?");
        
        ColumnMetadata idColumn = metadata.getColumns().get(metadata.getIdField().getName());
        List<Object> parameters = List.of(idColumn.getValue(entity));
        
        return new PreparedQuery(sql.toString(), parameters);
    }
    
    /**
     * Build SELECT query for finding by foreign key.
     */
    public static PreparedQuery buildSelectByForeignKey(EntityMetadata metadata, String foreignKeyColumn, Object foreignKeyValue) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        
        // Add columns
        StringJoiner columns = new StringJoiner(", ");
        for (ColumnMetadata column : metadata.getColumns().values()) {
            columns.add(column.getColumnName());
        }
        sql.append(columns);
        
        sql.append(" FROM ");
        if (metadata.getSchema() != null) {
            sql.append(metadata.getSchema()).append(".");
        }
        sql.append(metadata.getTableName());
        
        sql.append(" WHERE ").append(foreignKeyColumn).append(" = ?");
        
        return new PreparedQuery(sql.toString(), List.of(foreignKeyValue));
    }
    
    /**
     * Represents a prepared SQL query with parameters.
     */
    public static class PreparedQuery {
        private final String sql;
        private final List<Object> parameters;
        
        public PreparedQuery(String sql, List<Object> parameters) {
            this.sql = sql;
            this.parameters = parameters != null ? parameters : List.of();
        }
        
        public String getSql() {
            return sql;
        }
        
        public List<Object> getParameters() {
            return parameters;
        }
        
        @Override
        public String toString() {
            return "PreparedQuery{sql='" + sql + "', parameters=" + parameters + "}";
        }
    }
}