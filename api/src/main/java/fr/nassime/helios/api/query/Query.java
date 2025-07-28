package fr.nassime.helios.api.query;

import java.util.List;
import java.util.Optional;

/**
 * Generic query interface for building and executing queries.
 */
public interface Query<T> {
    
    /**
     * Add a WHERE condition using field name and value.
     */
    Query<T> where(String field, Object value);
    
    /**
     * Add a WHERE condition with an operator.
     */
    Query<T> where(String field, QueryOperator operator, Object value);
    
    /**
     * Add an AND condition.
     */
    Query<T> and(String field, Object value);
    
    /**
     * Add an AND condition with an operator.
     */
    Query<T> and(String field, QueryOperator operator, Object value);
    
    /**
     * Add an OR condition.
     */
    Query<T> or(String field, Object value);
    
    /**
     * Add an OR condition with an operator.
     */
    Query<T> or(String field, QueryOperator operator, Object value);
    
    /**
     * Add an ORDER BY clause.
     */
    Query<T> orderBy(String field);
    
    /**
     * Add an ORDER BY clause with direction.
     */
    Query<T> orderBy(String field, SortDirection direction);
    
    /**
     * Set the maximum number of results.
     */
    Query<T> limit(int maxResults);
    
    /**
     * Set the offset for pagination.
     */
    Query<T> offset(int offset);
    
    /**
     * Execute the query and return all results.
     */
    List<T> getResultList();
    
    /**
     * Execute the query and return a single result.
     */
    Optional<T> getSingleResult();
    
    /**
     * Execute the query and return the first result.
     */
    Optional<T> getFirstResult();
    
    /**
     * Count the number of results without fetching them.
     */
    long count();
}