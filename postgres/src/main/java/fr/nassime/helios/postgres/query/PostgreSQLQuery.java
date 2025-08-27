package fr.nassime.helios.postgres.query;

import fr.nassime.helios.postgres.session.PostgreSQLSession;
import fr.nassime.helios.sql.query.AbstractSqlQuery;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.util.function.Function;

/**
 * PostgreSQL implementation of Query interface.
 * Extends AbstractSqlQuery with PostgreSQL-specific connection handling.
 */
@Slf4j
public class PostgreSQLQuery<T> extends AbstractSqlQuery<T> {
    
    private final PostgreSQLSession session;
    
    /**
     * Constructor for entity-based queries.
     */
    public PostgreSQLQuery(Class<T> entityClass, PostgreSQLSession session) {
        super(entityClass);
        this.session = session;
    }
    
    /**
     * Constructor for raw SQL queries (placeholder).
     */
    public PostgreSQLQuery(Class<T> entityClass, PostgreSQLSession session, String queryString) {
        super(entityClass);
        this.session = session;
        // For now, custom query strings are not implemented
        // This constructor is needed for the createSqlQuery method in AbstractSqlSession
    }
    
    /**
     * Execute database operation using PostgreSQL session's connection management.
     */
    @Override
    protected <R> R executeWithConnection(Function<Connection, R> operation) {
        return session.executeWithConnection(operation);
    }
}