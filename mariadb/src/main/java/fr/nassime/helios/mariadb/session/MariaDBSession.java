package fr.nassime.helios.mariadb.session;

import fr.nassime.helios.api.query.Query;
import fr.nassime.helios.api.transaction.Transaction;
import fr.nassime.helios.mariadb.query.MariaDBQuery;
import fr.nassime.helios.mariadb.transaction.MariaDBTransaction;
import fr.nassime.helios.sql.session.AbstractSqlSession;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;

/**
 * MariaDB implementation of HeliosSession.
 * Extends AbstractSqlSession to inherit common SQL functionality.
 */
@Slf4j
public class MariaDBSession extends AbstractSqlSession {

    public MariaDBSession(HikariDataSource dataSource) {
        super(dataSource);
        log.debug("MariaDB session created");
    }

    @Override
    protected Query createSqlQuery(String queryString) {
        return new MariaDBQuery(queryString, dataSource);
    }

    @Override
    protected <T> Query<T> createEntityQuery(Class<T> entityClass) {
        return new MariaDBQuery<>(entityClass, this, dataSource);
    }

    @Override
    protected Transaction createTransaction(Connection connection) {
        return new MariaDBTransaction(connection);
    }
}