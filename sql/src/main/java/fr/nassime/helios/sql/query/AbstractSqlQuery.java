package fr.nassime.helios.sql.query;

import fr.nassime.helios.api.exception.HeliosException;
import fr.nassime.helios.api.query.Query;
import fr.nassime.helios.api.query.QueryOperator;
import fr.nassime.helios.api.query.SortDirection;
import fr.nassime.helios.sql.mapping.EntityMapper;
import fr.nassime.helios.sql.mapping.EntityMetadata;
import fr.nassime.helios.sql.mapping.ResultSetMapper;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Abstract base class for SQL-based Query implementations.
 * Contains all common query building and execution logic shared between database providers.
 */
@Slf4j
public abstract class AbstractSqlQuery<T> implements Query<T> {
    
    protected final Class<T> entityClass;
    protected final List<WhereCondition> whereConditions = new ArrayList<>();
    protected final List<OrderCondition> orderConditions = new ArrayList<>();
    protected Integer limitValue;
    protected Integer offsetValue;
    
    protected AbstractSqlQuery(Class<T> entityClass) {
        this.entityClass = entityClass;
    }
    
    /**
     * Database-specific connection execution.
     * Each implementation provides its own way to execute operations with connections.
     */
    protected abstract <R> R executeWithConnection(Function<Connection, R> operation);
    
    // ==================== QUERY BUILDING METHODS ====================
    
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
    
    // ==================== RESULT EXECUTION METHODS ====================
    
    @Override
    public List<T> getResultList() {
        String sql = buildSelectQuery();
        List<Object> parameters = buildParameters();
        
        log.debug("Executing query: {} with parameters: {}", sql, parameters);
        
        return executeWithConnection(connection -> {
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                setParameters(stmt, parameters);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    ResultSetMapper mapper = new ResultSetMapper();
                    List<T> results = mapper.mapToList(rs, entityClass);
                    log.debug("Query returned {} results", results.size());
                    return results;
                }
            } catch (SQLException e) {
                throw new HeliosException("Failed to execute query", e);
            }
        });
    }
    
    @Override
    public Optional<T> getSingleResult() {
        List<T> results = getResultList();
        if (results.isEmpty()) {
            return Optional.empty();
        }
        if (results.size() > 1) {
            throw new IllegalStateException("Query returned multiple results, expected single result");
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
        
        log.debug("Executing count query: {} with parameters: {}", sql, parameters);
        
        return executeWithConnection(connection -> {
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                setParameters(stmt, parameters);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        long count = rs.getLong(1);
                        log.debug("Count query returned: {}", count);
                        return count;
                    }
                    return 0L;
                }
            } catch (SQLException e) {
                throw new HeliosException("Failed to execute count query", e);
            }
        });
    }
    
    // ==================== SQL GENERATION METHODS ====================
    
    /**
     * Build SELECT query for retrieving entities.
     */
    protected String buildSelectQuery() {
        StringBuilder sql = new StringBuilder();
        
        // SELECT clause
        sql.append("SELECT * FROM ");
        sql.append(getTableName());
        
        // WHERE clause
        buildWhereClause(sql);
        
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
     * Build COUNT query for counting entities.
     */
    protected String buildCountQuery() {
        StringBuilder sql = new StringBuilder();
        
        // SELECT COUNT clause
        sql.append("SELECT COUNT(*) FROM ");
        sql.append(getTableName());
        
        // WHERE clause (same as select, but no ORDER BY, LIMIT, OFFSET for count)
        buildWhereClause(sql);
        
        return sql.toString();
    }
    
    /**
     * Build WHERE clause from conditions.
     */
    protected void buildWhereClause(StringBuilder sql) {
        if (whereConditions.isEmpty()) {
            return;
        }
        
        sql.append(" WHERE ");
        
        for (int i = 0; i < whereConditions.size(); i++) {
            WhereCondition condition = whereConditions.get(i);
            
            // Add logical operator (except for first condition)
            if (i > 0) {
                sql.append(" ").append(condition.logicalOperator.name()).append(" ");
            }
            
            // Add field condition
            sql.append(condition.field);
            
            // Add operator
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
                    sql.append(" IN (");
                    if (condition.value instanceof List<?> list) {
                        for (int j = 0; j < list.size(); j++) {
                            if (j > 0) sql.append(", ");
                            sql.append("?");
                        }
                    } else {
                        sql.append("?");
                    }
                    sql.append(")");
                }
                case NOT_IN -> {
                    sql.append(" NOT IN (");
                    if (condition.value instanceof List<?> list) {
                        for (int j = 0; j < list.size(); j++) {
                            if (j > 0) sql.append(", ");
                            sql.append("?");
                        }
                    } else {
                        sql.append("?");
                    }
                    sql.append(")");
                }
                case IS_NULL -> sql.append(" IS NULL");
                case IS_NOT_NULL -> sql.append(" IS NOT NULL");
                case BETWEEN -> {
                    if (condition.value instanceof Object[] range && range.length == 2) {
                        sql.append(" BETWEEN ? AND ?");
                    } else {
                        throw new IllegalArgumentException("BETWEEN operator requires an array of exactly 2 values");
                    }
                }
                case NOT_BETWEEN -> {
                    if (condition.value instanceof Object[] range && range.length == 2) {
                        sql.append(" NOT BETWEEN ? AND ?");
                    } else {
                        throw new IllegalArgumentException("NOT_BETWEEN operator requires an array of exactly 2 values");
                    }
                }
            }
        }
    }
    
    /**
     * Build parameter list for prepared statement.
     */
    protected List<Object> buildParameters() {
        List<Object> parameters = new ArrayList<>();
        
        for (WhereCondition condition : whereConditions) {
            // Skip NULL checks as they don't need parameters
            if (condition.operator == QueryOperator.IS_NULL || condition.operator == QueryOperator.IS_NOT_NULL) {
                continue;
            }
            
            if ((condition.operator == QueryOperator.IN || condition.operator == QueryOperator.NOT_IN) 
                && condition.value instanceof List<?> list) {
                parameters.addAll(list);
            } else if ((condition.operator == QueryOperator.BETWEEN || condition.operator == QueryOperator.NOT_BETWEEN)
                && condition.value instanceof Object[] range && range.length == 2) {
                parameters.add(range[0]);
                parameters.add(range[1]);
            } else {
                parameters.add(condition.value);
            }
        }
        
        return parameters;
    }
    
    /**
     * Get table name for the entity class.
     */
    protected String getTableName() {
        EntityMetadata metadata = EntityMapper.getMetadata(entityClass);
        return metadata.getTableName();
    }
    
    /**
     * Set parameters on prepared statement.
     */
    protected void setParameters(PreparedStatement stmt, List<Object> parameters) throws SQLException {
        for (int i = 0; i < parameters.size(); i++) {
            stmt.setObject(i + 1, parameters.get(i));
        }
    }
    
    // ==================== INNER CLASSES ====================
    
    /**
     * Represents a WHERE condition.
     */
    protected static class WhereCondition {
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
    protected static class OrderCondition {
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
    protected enum LogicalOperator {
        AND, OR
    }
}