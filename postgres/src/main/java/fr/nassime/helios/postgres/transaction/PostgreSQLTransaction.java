package fr.nassime.helios.postgres.transaction;

import fr.nassime.helios.sql.transaction.AbstractSqlTransaction;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;

/**
 * PostgreSQL implementation of Transaction.
 * Extends AbstractSqlTransaction to inherit common SQL transaction functionality.
 */
@Slf4j
public class PostgreSQLTransaction extends AbstractSqlTransaction {
    
    public PostgreSQLTransaction(Connection connection) {
        super(connection);
        log.debug("PostgreSQL transaction initialized");
    }
}