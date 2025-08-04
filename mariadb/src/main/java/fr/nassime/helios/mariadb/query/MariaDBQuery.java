package fr.nassime.helios.mariadb.query;

import fr.nassime.helios.api.exception.HeliosException;
import fr.nassime.helios.api.query.Query;
import fr.nassime.helios.api.query.QueryOperator;
import fr.nassime.helios.api.query.SortDirection;
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
    
    private final StringBuilder sql;
    private final HikariDataSource dataSource;
    private final List<Object> parameters;
    private final Class<T> entityClass;
    
    public MariaDBQuery(String baseSql, HikariDataSource dataSource) {
        this.sql = new StringBuilder(baseSql);
        this.dataSource = dataSource;
        this.parameters = new ArrayList<>();
        this.entityClass = null; // For raw SQL queries
    }
    
    @Override
    public Query<T> where(String field, Object value) {
        return where(field, QueryOperator.EQUALS, value);
    }
    
    @Override
    public Query<T> where(String field, QueryOperator operator, Object value) {
        sql.append(" WHERE ").append(field).append(" ").append(operator.getSql()).append(" ?");
        parameters.add(value);
        return this;
    }
    
    @Override
    public Query<T> and(String field, Object value) {
        return and(field, QueryOperator.EQUALS, value);
    }
    
    @Override
    public Query<T> and(String field, QueryOperator operator, Object value) {
        sql.append(" AND ").append(field).append(" ").append(operator.getSql()).append(" ?");
        parameters.add(value);
        return this;
    }
    
    @Override
    public Query<T> or(String field, Object value) {
        return or(field, QueryOperator.EQUALS, value);
    }
    
    @Override
    public Query<T> or(String field, QueryOperator operator, Object value) {
        sql.append(" OR ").append(field).append(" ").append(operator.getSql()).append(" ?");
        parameters.add(value);
        return this;
    }
    
    @Override
    public Query<T> orderBy(String field) {
        return orderBy(field, SortDirection.ASC);
    }
    
    @Override
    public Query<T> orderBy(String field, SortDirection direction) {
        sql.append(" ORDER BY ").append(field).append(" ").append(direction.name());
        return this;
    }
    
    @Override
    public Query<T> limit(int maxResults) {
        sql.append(" LIMIT ").append(maxResults);
        return this;
    }
    
    @Override
    public Query<T> offset(int offset) {
        sql.append(" OFFSET ").append(offset);
        return this;
    }
    
    @Override
    public List<T> getResultList() {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql.toString())) {
            
            // Set parameters
            for (int i = 0; i < parameters.size(); i++) {
                stmt.setObject(i + 1, parameters.get(i));
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                List<T> results = new ArrayList<>();
                while (rs.next()) {
                    // For now, return null - full implementation would map ResultSet to entity
                    results.add(null);
                }
                return results;
            }
        } catch (SQLException e) {
            throw new HeliosException("Failed to execute query", e);
        }
    }
    
    @Override
    public Optional<T> getSingleResult() {
        List<T> results = getResultList();
        if (results.isEmpty()) {
            return Optional.empty();
        }
        if (results.size() > 1) {
            throw new HeliosException("Query returned more than one result");
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
        // Replace the main SELECT with COUNT(*)
        String countSql = sql.toString().replaceFirst("SELECT .* FROM", "SELECT COUNT(*) FROM");
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(countSql)) {
            
            // Set parameters
            for (int i = 0; i < parameters.size(); i++) {
                stmt.setObject(i + 1, parameters.get(i));
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                return 0;
            }
        } catch (SQLException e) {
            throw new HeliosException("Failed to execute count query", e);
        }
    }
}