package fr.nassime.helios.mariadb.query;

import fr.nassime.helios.api.exception.HeliosException;
import fr.nassime.helios.api.query.Query;
import fr.nassime.helios.api.query.QueryOperator;
import fr.nassime.helios.api.query.SortDirection;
import fr.nassime.helios.mariadb.session.MariaDBSession;
import fr.nassime.helios.sql.mapping.EntityMapper;
import fr.nassime.helios.sql.mapping.EntityMetadata;
import fr.nassime.helios.sql.mapping.ResultSetMapper;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * MariaDB implementation of Query interface.
 */
@Slf4j
public class MariaDBQuery<T> implements Query<T> {
    
    private final Class<T> entityClass;
    private final MariaDBSession session;
    private final HikariDataSource dataSource;
    private final List<WhereCondition> whereConditions = new ArrayList<>();
    private final List<OrderCondition> orderConditions = new ArrayList<>();
    private Integer limitValue;
    private Integer offsetValue;
    
    // Constructor for entity-based queries
    public MariaDBQuery(Class<T> entityClass, MariaDBSession session, HikariDataSource dataSource) {
        this.entityClass = entityClass;
        this.session = session;
        this.dataSource = dataSource;
    }
    
    // Constructor for raw SQL queries
    public MariaDBQuery(String baseSql, HikariDataSource dataSource) {
        this.entityClass = null;
        this.session = null;
        this.dataSource = dataSource;
        // For now, custom query strings are not implemented
        // This constructor is needed for the createSqlQuery method in AbstractSqlSession
    }
    
    @Override
    public Query<T> where(String field, Object value) {
        return where(field, QueryOperator.EQUALS, value);
    }
    
    @Override
    public Query<T> where(String field, QueryOperator operator, Object value) {
        whereConditions.add(new WhereCondition(field, operator, value, LogicalOperator.AND));
        return this;
    }
    
    @Override
    public Query<T> and(String field, Object value) {
        return and(field, QueryOperator.EQUALS, value);
    }
    
    @Override
    public Query<T> and(String field, QueryOperator operator, Object value) {
        if (whereConditions.isEmpty()) {
            return where(field, operator, value);
        }
        whereConditions.add(new WhereCondition(field, operator, value, LogicalOperator.AND));
        return this;
    }
    
    @Override
    public Query<T> or(String field, Object value) {
        return or(field, QueryOperator.EQUALS, value);
    }
    
    @Override
    public Query<T> or(String field, QueryOperator operator, Object value) {
        if (whereConditions.isEmpty()) {
            return where(field, operator, value);
        }
        whereConditions.add(new WhereCondition(field, operator, value, LogicalOperator.OR));
        return this;
    }
    
    @Override
    public Query<T> orderBy(String field) {
        return orderBy(field, SortDirection.ASC);
    }
    
    @Override
    public Query<T> orderBy(String field, SortDirection direction) {
        orderConditions.add(new OrderCondition(field, direction));
        return this;
    }
    
    @Override
    public Query<T> limit(int maxResults) {
        this.limitValue = maxResults;
        return this;
    }
    
    @Override
    public Query<T> offset(int offset) {
        this.offsetValue = offset;
        return this;
    }
    
    @Override
    public List<T> getResultList() {
        String sql = buildSelectQuery();
        List<Object> parameters = buildParameters();
        
        log.debug("Executing query: {} for entity {}", sql, entityClass.getSimpleName());
        
        return session.executeWithConnection(connection -> {
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                // Set parameters
                for (int i = 0; i < parameters.size(); i++) {
                    stmt.setObject(i + 1, parameters.get(i));
                }
                
                try (ResultSet rs = stmt.executeQuery()) {
                    ResultSetMapper mapper = new ResultSetMapper();
                    return mapper.mapToList(rs, entityClass);
                }
            } catch (SQLException e) {
                throw new HeliosException("Failed to execute query", e);
            }
        });
    }
    
    @Override
    public Optional<T> getSingleResult() {
        List<T> results = limit(2).getResultList();
        
        if (results.isEmpty()) {
            return Optional.empty();
        }
        
        if (results.size() > 1) {
            throw new IllegalStateException("Query returned more than one result");
        }
        
        return Optional.of(results.get(0));
    }
    
    @Override
    public Optional<T> getFirstResult() {
        List<T> results = limit(1).getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
    
    @Override
    public long count() {
        String sql = buildCountQuery();
        List<Object> parameters = buildParameters();
        
        log.debug("Executing count query: {} for entity {}", sql, entityClass.getSimpleName());
        
        return session.executeWithConnection(connection -> {
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                // Set parameters
                for (int i = 0; i < parameters.size(); i++) {
                    stmt.setObject(i + 1, parameters.get(i));
                }
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getLong(1);
                    }
                    return 0L;
                }
            } catch (SQLException e) {
                throw new HeliosException("Failed to execute count query", e);
            }
        });
    }
    
    /**
     * Build the SQL SELECT query.
     */
    private String buildSelectQuery() {
        StringBuilder sql = new StringBuilder();
        
        EntityMetadata metadata = EntityMapper.getMetadata(entityClass);
        
        // SELECT clause with all columns
        sql.append("SELECT ");
        List<String> columns = new ArrayList<>();
        for (String columnName : metadata.getColumnNames()) {
            columns.add(columnName);
        }
        sql.append(String.join(", ", columns));
        
        // FROM clause
        sql.append(" FROM ");
        if (metadata.getSchema() != null) {
            sql.append(metadata.getSchema()).append(".");
        }
        sql.append(metadata.getTableName());
        
        // WHERE clause
        if (!whereConditions.isEmpty()) {
            sql.append(" WHERE ");
            buildWhereClause(sql);
        }
        
        // ORDER BY clause
        if (!orderConditions.isEmpty()) {
            sql.append(" ORDER BY ");
            for (int i = 0; i < orderConditions.size(); i++) {
                if (i > 0) {
                    sql.append(", ");
                }
                OrderCondition order = orderConditions.get(i);
                sql.append(order.field).append(" ").append(order.direction.name());
            }
        }
        
        // LIMIT clause
        if (limitValue != null) {
            sql.append(" LIMIT ").append(limitValue);
        }
        
        // OFFSET clause
        if (offsetValue != null) {
            sql.append(" OFFSET ").append(offsetValue);
        }
        
        return sql.toString();
    }
    
    /**
     * Build the SQL COUNT query.
     */
    private String buildCountQuery() {
        StringBuilder sql = new StringBuilder();
        
        String tableName = getTableName();
        sql.append("SELECT COUNT(*) FROM ").append(tableName);
        
        // WHERE clause
        if (!whereConditions.isEmpty()) {
            sql.append(" WHERE ");
            buildWhereClause(sql);
        }
        
        return sql.toString();
    }
    
    /**
     * Build the WHERE clause.
     */
    private void buildWhereClause(StringBuilder sql) {
        for (int i = 0; i < whereConditions.size(); i++) {
            WhereCondition condition = whereConditions.get(i);
            
            if (i > 0) {
                sql.append(" ").append(condition.logicalOperator.name()).append(" ");
            }
            
            sql.append(condition.field);
            
            switch (condition.operator) {
                case EQUALS -> sql.append(" = ?");
                case NOT_EQUALS -> sql.append(" != ?");
                case GREATER_THAN -> sql.append(" > ?");
                case GREATER_THAN_OR_EQUAL -> sql.append(" >= ?");
                case LESS_THAN -> sql.append(" < ?");
                case LESS_THAN_OR_EQUAL -> sql.append(" <= ?");
                case LIKE -> sql.append(" LIKE ?");
                case NOT_LIKE -> sql.append(" NOT LIKE ?");
                case IN -> {
                    if (condition.value instanceof java.util.Collection<?> collection) {
                        String placeholders = String.join(",", java.util.Collections.nCopies(collection.size(), "?"));
                        sql.append(" IN (").append(placeholders).append(")");
                    } else {
                        sql.append(" IN (?)");
                    }
                }
                case NOT_IN -> {
                    if (condition.value instanceof java.util.Collection<?> collection) {
                        String placeholders = String.join(",", java.util.Collections.nCopies(collection.size(), "?"));
                        sql.append(" NOT IN (").append(placeholders).append(")");
                    } else {
                        sql.append(" NOT IN (?)");
                    }
                }
                case IS_NULL -> sql.append(" IS NULL");
                case IS_NOT_NULL -> sql.append(" IS NOT NULL");
                case BETWEEN -> {
                    if (condition.value instanceof Object[] range && range.length == 2) {
                        sql.append(" BETWEEN ? AND ?");
                    } else {
                        throw new IllegalArgumentException("BETWEEN operator requires an array of 2 values");
                    }
                }
                case NOT_BETWEEN -> {
                    if (condition.value instanceof Object[] range && range.length == 2) {
                        sql.append(" NOT BETWEEN ? AND ?");
                    } else {
                        throw new IllegalArgumentException("NOT_BETWEEN operator requires an array of 2 values");
                    }
                }
            }
        }
    }
    
    /**
     * Build parameter list for prepared statement.
     */
    private List<Object> buildParameters() {
        List<Object> parameters = new ArrayList<>();
        
        for (WhereCondition condition : whereConditions) {
            switch (condition.operator) {
                case IS_NULL, IS_NOT_NULL -> {
                    // No parameters needed for these operators
                }
                case IN, NOT_IN -> {
                    if (condition.value instanceof java.util.Collection<?> collection) {
                        parameters.addAll(collection);
                    } else {
                        parameters.add(condition.value);
                    }
                }
                case BETWEEN, NOT_BETWEEN -> {
                    if (condition.value instanceof Object[] range && range.length == 2) {
                        parameters.add(range[0]);
                        parameters.add(range[1]);
                    } else {
                        throw new IllegalArgumentException("BETWEEN/NOT_BETWEEN operator requires an array of 2 values");
                    }
                }
                default -> parameters.add(condition.value);
            }
        }
        
        return parameters;
    }
    
    /**
     * Get table name for the entity class.
     */
    private String getTableName() {
        EntityMetadata metadata = EntityMapper.getMetadata(entityClass);
        return metadata.getTableName();
    }
    
    /**
     * Represents a WHERE condition.
     */
    private static class WhereCondition {
        final String field;
        final QueryOperator operator;
        final Object value;
        final LogicalOperator logicalOperator;
        
        WhereCondition(String field, QueryOperator operator, Object value, LogicalOperator logicalOperator) {
            this.field = field;
            this.operator = operator;
            this.value = value;
            this.logicalOperator = logicalOperator;
        }
    }
    
    /**
     * Represents an ORDER BY condition.
     */
    private static class OrderCondition {
        final String field;
        final SortDirection direction;
        
        OrderCondition(String field, SortDirection direction) {
            this.field = field;
            this.direction = direction;
        }
    }
    
    /**
     * Logical operators for combining conditions.
     */
    private enum LogicalOperator {
        AND, OR
    }
}