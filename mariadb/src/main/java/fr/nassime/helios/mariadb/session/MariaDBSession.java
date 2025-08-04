package fr.nassime.helios.mariadb.session;

import fr.nassime.helios.api.exception.HeliosException;
import fr.nassime.helios.api.query.Query;
import fr.nassime.helios.mariadb.query.MariaDBQuery;
import fr.nassime.helios.sql.session.AbstractSqlSession;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Function;

/**
 * MariaDB implementation of HeliosSession.
 * Extends AbstractSqlSession to inherit common SQL functionality.
 */
@Slf4j
public class MariaDBSession extends AbstractSqlSession {
    
    private final HikariDataSource dataSource;
    
    public MariaDBSession(HikariDataSource dataSource) {
        super();
        this.dataSource = dataSource;
        log.debug("MariaDB session created");
    }
    
    @Override
    protected Query createSqlQuery(String queryString) {
        return new MariaDBQuery(queryString, dataSource);
    }
    
    @Override
    public <T> T executeWithConnection(Function<Connection, T> operation) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                T result = operation.apply(connection);
                connection.commit();
                return result;
            } catch (Exception e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new HeliosException("Database operation failed", e);
        }
    }
}