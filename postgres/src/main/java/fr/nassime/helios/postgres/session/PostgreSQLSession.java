package fr.nassime.helios.postgres.session;

import fr.nassime.helios.api.query.Query;
import fr.nassime.helios.api.transaction.Transaction;
import fr.nassime.helios.postgres.query.PostgreSQLQuery;
import fr.nassime.helios.postgres.sql.PostgreSQLSqlBuilder;
import fr.nassime.helios.postgres.transaction.PostgreSQLTransaction;
import fr.nassime.helios.sql.mapping.EntityMetadata;
import fr.nassime.helios.sql.query.SqlBuilder;
import fr.nassime.helios.sql.session.AbstractSqlSession;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;

/**
 * PostgreSQL implementation of HeliosSession.
 * Extends AbstractSqlSession to inherit common SQL functionality.
 */
@Slf4j
public class PostgreSQLSession extends AbstractSqlSession {

    public PostgreSQLSession(HikariDataSource dataSource) {
        super(dataSource);
        log.debug("PostgreSQL session created");
    }

    @Override
    protected SqlBuilder.PreparedQuery buildInsertQuery(EntityMetadata metadata, Object entity) {
        // Use PostgreSQL-specific insert builder with RETURNING clause
        return PostgreSQLSqlBuilder.buildInsert(metadata, entity);
    }

    @Override
    protected Query createSqlQuery(String queryString) {
        return new PostgreSQLQuery<>(Object.class, this, queryString);
    }

    @Override
    protected <T> Query<T> createEntityQuery(Class<T> entityClass) {
        return new PostgreSQLQuery<>(entityClass, this);
    }

    @Override
    protected Transaction createTransaction(Connection connection) {
        return new PostgreSQLTransaction(connection);
    }
}