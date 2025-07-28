package fr.nassime.helios.postgres.query;

import fr.nassime.helios.api.query.Query;
import fr.nassime.helios.api.query.QueryOperator;
import fr.nassime.helios.api.query.SortDirection;
import fr.nassime.helios.postgres.session.PostgreSQLSession;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * PostgreSQL implementation of Query interface.
 */
@Slf4j
public class PostgreSQLQuery<T> implements Query<T> {
    
    private final Class<T> entityClass;
    private final PostgreSQLSession session;
    private final List<WhereCondition> whereConditions = new ArrayList<>();
    private final List<OrderCondition> orderConditions = new ArrayList<>();
    private Integer limitValue;
    private Integer offsetValue;
    
    public PostgreSQLQuery(Class<T> entityClass, PostgreSQLSession session) {
        this.entityClass = entityClass;
        this.session = session;
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
        log.debug("Executing query: {} for entity {}", sql, entityClass.getSimpleName());
        
        // TODO: Execute query and map results
        // For now, returning empty list
        return List.of();
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
        log.debug("Executing count query: {} for entity {}", sql, entityClass.getSimpleName());
        
        // TODO: Execute count query
        // For now, returning 0
        return 0L;
    }
    
    /**
     * Build the SQL SELECT query.
     */
    private String buildSelectQuery() {
        StringBuilder sql = new StringBuilder();
        
        // TODO: Get table name from entity metadata
        String tableName = getTableName();
        
        sql.append("SELECT * FROM ").append(tableName);
        
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
                case IN -> sql.append(" IN (?)"); // TODO: Handle multiple values
                case NOT_IN -> sql.append(" NOT IN (?)"); // TODO: Handle multiple values
                case IS_NULL -> sql.append(" IS NULL");
                case IS_NOT_NULL -> sql.append(" IS NOT NULL");
                case BETWEEN -> sql.append(" BETWEEN ? AND ?"); // TODO: Handle range values
                case NOT_BETWEEN -> sql.append(" NOT BETWEEN ? AND ?"); // TODO: Handle range values
            }
        }
    }
    
    /**
     * Get table name for the entity class.
     */
    private String getTableName() {
        // TODO: Extract from @Entity annotation or use class name
        return entityClass.getSimpleName().toLowerCase() + "s";
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