package fr.nassime.helios.mariadb.transaction;

import fr.nassime.helios.sql.transaction.AbstractSqlTransaction;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;

/**
 * MariaDB implementation of Transaction.
 * Extends AbstractSqlTransaction to inherit common SQL transaction functionality.
 */
@Slf4j
public class MariaDBTransaction extends AbstractSqlTransaction {
    
    public MariaDBTransaction(Connection connection) {
        super(connection);
        log.debug("MariaDB transaction initialized");
    }
}