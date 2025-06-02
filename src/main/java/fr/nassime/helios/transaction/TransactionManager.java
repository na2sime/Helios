package fr.nassime.helios.transaction;

import fr.nassime.helios.exception.HeliosException;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Consumer;
import java.util.function.Function;

@Slf4j
public class TransactionManager {

    public <T> T executeInTransaction(Connection connection, Function<Connection, T> operation) {
        boolean previousAutoCommit = false;

        try {
            previousAutoCommit = connection.getAutoCommit();

            if (previousAutoCommit) {
                connection.setAutoCommit(false);
            }

            T result = operation.apply(connection);

            connection.commit();

            return result;
        } catch (Exception e) {
            try {
                connection.rollback();
                log.warn("Transaction canceled due to error", e);
            } catch (SQLException rollbackEx) {
                log.error("Error canceling fr.nassime.helios.transaction", rollbackEx);
            }

            throw new HeliosException("Error executing fr.nassime.helios.transaction", e);
        } finally {
            try {
                if (previousAutoCommit) {
                    connection.setAutoCommit(true);
                }
            } catch (SQLException e) {
                log.error("Error restoring autocommit", e);
            }
        }
    }

    public void executeInTransactionWithoutResult(Connection connection, Consumer<Connection> operation) {
        executeInTransaction(connection, conn -> {
            operation.accept(conn);
            return null;
        });
    }
}

