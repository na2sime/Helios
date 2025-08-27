package fr.nassime.helios.mongo.query;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import fr.nassime.helios.api.exception.HeliosException;
import fr.nassime.helios.api.query.Query;
import fr.nassime.helios.api.query.QueryOperator;
import fr.nassime.helios.api.query.SortDirection;
import fr.nassime.helios.mongo.mapping.DocumentMapper;
import fr.nassime.helios.mongo.session.MongoSession;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Sorts.*;

/**
 * MongoDB implementation of Query interface.
 * Provides fluent query building capabilities using MongoDB filters.
 */
@Slf4j
public class MongoQuery<T> implements Query<T> {
    
    private final Class<T> entityClass;
    private final MongoSession session;
    private final MongoDatabase database;
    private final DocumentMapper documentMapper;
    
    private final List<Bson> filters = new ArrayList<>();
    private final List<Bson> sorts = new ArrayList<>();
    private Integer limitValue;
    private Integer offsetValue;
    
    public MongoQuery(Class<T> entityClass, MongoSession session, MongoDatabase database) {
        this.entityClass = entityClass;
        this.session = session;
        this.database = database;
        this.documentMapper = session.getDocumentMapper();
    }
    
    // ==================== QUERY BUILDING METHODS ====================
    
    @Override
    public Query<T> where(String field, Object value) {
        return where(field, QueryOperator.EQUALS, value);
    }
    
    @Override
    public Query<T> where(String field, QueryOperator operator, Object value) {
        Bson filter = buildFilter(field, operator, value);
        filters.add(filter);
        return this;
    }
    
    @Override
    public Query<T> and(String field, Object value) {
        return and(field, QueryOperator.EQUALS, value);
    }
    
    @Override
    public Query<T> and(String field, QueryOperator operator, Object value) {
        // In MongoDB, adding filters sequentially creates an implicit AND
        return where(field, operator, value);
    }
    
    @Override
    public Query<T> or(String field, Object value) {
        return or(field, QueryOperator.EQUALS, value);
    }
    
    @Override
    public Query<T> or(String field, QueryOperator operator, Object value) {
        if (filters.isEmpty()) {
            // If no previous filters, just add the new one
            return where(field, operator, value);
        }
        
        // Create OR condition with existing filters
        Bson newFilter = buildFilter(field, operator, value);
        Bson existingFilters = filters.size() == 1 ? filters.get(0) : and(filters);
        
        filters.clear();
        filters.add(or(existingFilters, newFilter));
        return this;
    }
    
    @Override
    public Query<T> orderBy(String field) {
        return orderBy(field, SortDirection.ASC);
    }
    
    @Override
    public Query<T> orderBy(String field, SortDirection direction) {
        Bson sort = direction == SortDirection.ASC ? ascending(field) : descending(field);
        sorts.add(sort);
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
        log.debug("Executing MongoDB query for class: {}", entityClass.getSimpleName());
        
        try {
            String collectionName = documentMapper.getCollectionName(entityClass);
            MongoCollection<Document> collection = database.getCollection(collectionName);
            
            FindIterable<Document> findIterable = collection.find();
            
            // Apply filters
            if (!filters.isEmpty()) {
                Bson combinedFilter = filters.size() == 1 ? filters.get(0) : and(filters);
                findIterable = findIterable.filter(combinedFilter);
                log.debug("Applied filters: {}", combinedFilter);
            }
            
            // Apply sorting
            if (!sorts.isEmpty()) {
                Bson combinedSort = sorts.size() == 1 ? sorts.get(0) : orderBy(sorts);
                findIterable = findIterable.sort(combinedSort);
                log.debug("Applied sorting");
            }
            
            // Apply offset
            if (offsetValue != null && offsetValue > 0) {
                findIterable = findIterable.skip(offsetValue);
                log.debug("Applied offset: {}", offsetValue);
            }
            
            // Apply limit
            if (limitValue != null && limitValue > 0) {
                findIterable = findIterable.limit(limitValue);
                log.debug("Applied limit: {}", limitValue);
            }
            
            // Convert documents to entities
            List<T> results = new ArrayList<>();
            for (Document document : findIterable) {
                T entity = documentMapper.fromDocument(document, entityClass);
                results.add(entity);
            }
            
            log.debug("Query returned {} results", results.size());
            return results;
            
        } catch (Exception e) {
            log.error("Failed to execute MongoDB query for class: {}", entityClass.getSimpleName(), e);
            throw new HeliosException("Failed to execute query: " + e.getMessage(), e);
        }
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
        log.debug("Executing MongoDB count query for class: {}", entityClass.getSimpleName());
        
        try {
            String collectionName = documentMapper.getCollectionName(entityClass);
            MongoCollection<Document> collection = database.getCollection(collectionName);
            
            long count;
            if (filters.isEmpty()) {
                count = collection.countDocuments();
            } else {
                Bson combinedFilter = filters.size() == 1 ? filters.get(0) : and(filters);
                count = collection.countDocuments(combinedFilter);
            }
            
            log.debug("Count query returned: {}", count);
            return count;
            
        } catch (Exception e) {
            log.error("Failed to execute MongoDB count query for class: {}", entityClass.getSimpleName(), e);
            throw new HeliosException("Failed to execute count query: " + e.getMessage(), e);
        }
    }
    
    // ==================== HELPER METHODS ====================
    
    /**
     * Build MongoDB filter from operator and value.
     */
    private Bson buildFilter(String field, QueryOperator operator, Object value) {
        return switch (operator) {
            case EQUALS -> eq(field, value);
            case NOT_EQUALS -> ne(field, value);
            case GREATER_THAN -> gt(field, value);
            case GREATER_THAN_OR_EQUAL -> gte(field, value);
            case LESS_THAN -> lt(field, value);
            case LESS_THAN_OR_EQUAL -> lte(field, value);
            case LIKE -> regex(field, value.toString());
            case NOT_LIKE -> not(regex(field, value.toString()));
            case IN -> {
                if (value instanceof List<?> list) {
                    yield in(field, list);
                } else {
                    yield in(field, value);
                }
            }
            case NOT_IN -> {
                if (value instanceof List<?> list) {
                    yield nin(field, list);
                } else {
                    yield nin(field, value);
                }
            }
            case IS_NULL -> eq(field, null);
            case IS_NOT_NULL -> ne(field, null);
            case BETWEEN -> {
                if (value instanceof Object[] range && range.length == 2) {
                    yield and(gte(field, range[0]), lte(field, range[1]));
                } else {
                    throw new IllegalArgumentException("BETWEEN operator requires an array of exactly 2 values");
                }
            }
            case NOT_BETWEEN -> {
                if (value instanceof Object[] range && range.length == 2) {
                    yield or(lt(field, range[0]), gt(field, range[1]));
                } else {
                    throw new IllegalArgumentException("NOT_BETWEEN operator requires an array of exactly 2 values");
                }
            }
        };
    }
}