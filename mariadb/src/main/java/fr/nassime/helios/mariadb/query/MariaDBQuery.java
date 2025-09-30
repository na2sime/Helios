package fr.nassime.helios.mariadb.query;

import fr.nassime.helios.mariadb.session.MariaDBSession;
import fr.nassime.helios.sql.query.AbstractSqlQuery;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.util.function.Function;

/**
 * MariaDB implementation of Query interface.
 * Extends AbstractSqlQuery with MariaDB-specific connection handling.
 */
@Slf4j
public class MariaDBQuery<T> extends AbstractSqlQuery<T> {
    
    private final MariaDBSession session;
    private final HikariDataSource dataSource;
    
    /**
     * Constructor for entity-based queries.
     */
    public MariaDBQuery(Class<T> entityClass, MariaDBSession session, HikariDataSource dataSource) {
        super(entityClass);
        this.session = session;
        this.dataSource = dataSource;
    }
    
    /**
     * Constructor for raw SQL queries (placeholder).
     */
    public MariaDBQuery(String baseSql, HikariDataSource dataSource) {
        super(null); // Raw SQL queries don't have entity class
        this.session = null;
        this.dataSource = dataSource;
        // For now, custom query strings are not implemented
        // This constructor is needed for the createSqlQuery method in AbstractSqlSession
    }
    
    /**
     * Execute database operation using MariaDB session's connection management.
     */
    @Override
    protected <R> R executeWithConnection(Function<Connection, R> operation) {
        return session.executeWithConnection(operation);
    }
}