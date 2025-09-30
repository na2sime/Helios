package fr.nassime.helios.sql.transaction;

import fr.nassime.helios.api.exception.TransactionException;
import fr.nassime.helios.api.transaction.Transaction;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Abstract base class for SQL-based transactions.
 * Provides common transaction management functionality for all SQL database implementations.
 */
@Slf4j
public abstract class AbstractSqlTransaction implements Transaction {

    /**
     * -- GETTER --
     *  Get the underlying database connection.
     */
    @Getter
    private final Connection connection;
    private boolean active;
    private boolean rolledBack;
    private boolean committed;
    
    protected AbstractSqlTransaction(Connection connection) {
        this.connection = connection;
        this.active = true;
        this.rolledBack = false;
        this.committed = false;
        
        try {
            // Disable auto-commit for transaction management
            connection.setAutoCommit(false);
            log.debug("Transaction started");
        } catch (SQLException e) {
            throw new TransactionException("Failed to start transaction", e);
        }
    }
    
    @Override
    public void commit() {
        checkTransactionState();
        
        try {
            connection.commit();
            committed = true;
            active = false;
            log.debug("Transaction committed");
        } catch (SQLException e) {
            throw new TransactionException("Failed to commit transaction", e);
        } finally {
            restoreAutoCommit();
        }
    }
    
    @Override
    public void rollback() {
        if (!active) {
            log.warn("Attempted to rollback inactive transaction");
            return;
        }
        
        try {
            connection.rollback();
            rolledBack = true;
            active = false;
            log.debug("Transaction rolled back");
        } catch (SQLException e) {
            throw new TransactionException("Failed to rollback transaction", e);
        } finally {
            restoreAutoCommit();
        }
    }
    
    @Override
    public boolean isActive() {
        return active;
    }
    
    @Override
    public boolean isRollbackOnly() {
        return false; // Default implementation - can be overridden by subclasses
    }
    
    @Override
    public void setRollbackOnly() {
        // Default implementation - can be overridden by subclasses
    }

    /**
     * Check if the transaction is in a valid state for operations.
     */
    private void checkTransactionState() {
        if (!active) {
            throw new TransactionException("Transaction is not active");
        }
        if (committed) {
            throw new TransactionException("Transaction has already been committed");
        }
        if (rolledBack) {
            throw new TransactionException("Transaction has been rolled back");
        }
    }
    
    /**
     * Restore auto-commit mode after transaction completion.
     */
    private void restoreAutoCommit() {
        try {
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            log.warn("Failed to restore auto-commit mode", e);
        }
    }
    
}