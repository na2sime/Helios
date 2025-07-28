package fr.nassime.helios.postgres.transaction;

import fr.nassime.helios.api.exception.TransactionException;
import fr.nassime.helios.api.transaction.Transaction;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * PostgreSQL implementation of Transaction.
 */
@Slf4j
public class PostgreSQLTransaction implements Transaction {
    
    private final Connection connection;
    private boolean active = true;
    private boolean rollbackOnly = false;
    private boolean committed = false;
    private boolean rolledBack = false;
    
    public PostgreSQLTransaction(Connection connection) {
        this.connection = connection;
        try {
            connection.setAutoCommit(false);
            log.debug("PostgreSQL transaction started");
        } catch (SQLException e) {
            throw new TransactionException("Failed to start transaction", e);
        }
    }
    
    @Override
    public void commit() {
        if (!active) {
            throw new TransactionException("Transaction is not active");
        }
        
        if (rollbackOnly) {
            rollback();
            throw new TransactionException("Transaction marked for rollback only");
        }
        
        try {
            connection.commit();
            committed = true;
            active = false;
            log.debug("PostgreSQL transaction committed");
        } catch (SQLException e) {
            try {
                connection.rollback();
                rolledBack = true;
            } catch (SQLException rollbackEx) {
                log.error("Failed to rollback after commit failure", rollbackEx);
            }
            active = false;
            throw new TransactionException("Failed to commit transaction", e);
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                log.warn("Failed to reset auto-commit", e);
            }
        }
    }
    
    @Override
    public void rollback() {
        if (!active) {
            throw new TransactionException("Transaction is not active");
        }
        
        try {
            connection.rollback();
            rolledBack = true;
            active = false;
            log.debug("PostgreSQL transaction rolled back");
        } catch (SQLException e) {
            active = false;
            throw new TransactionException("Failed to rollback transaction", e);
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                log.warn("Failed to reset auto-commit", e);
            }
        }
    }
    
    @Override
    public boolean isActive() {
        return active;
    }
    
    @Override
    public boolean isRollbackOnly() {
        return rollbackOnly;
    }
    
    @Override
    public void setRollbackOnly() {
        if (!active) {
            throw new TransactionException("Transaction is not active");
        }
        this.rollbackOnly = true;
        log.debug("PostgreSQL transaction marked for rollback only");
    }
    
    /**
     * Get the underlying connection.
     */
    public Connection getConnection() {
        return connection;
    }
    
    /**
     * Check if the transaction was committed.
     */
    public boolean isCommitted() {
        return committed;
    }
    
    /**
     * Check if the transaction was rolled back.
     */
    public boolean isRolledBack() {
        return rolledBack;
    }
}