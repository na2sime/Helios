package fr.nassime.helios.mongo.transaction;

import com.mongodb.TransactionOptions;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import fr.nassime.helios.api.exception.HeliosException;
import fr.nassime.helios.api.transaction.Transaction;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * MongoDB implementation of Transaction interface using MongoDB sessions.
 * Provides ACID transaction support for MongoDB operations.
 */
@Slf4j
public class MongoTransaction implements Transaction, AutoCloseable {
    
    private final MongoClient mongoClient;
    /**
     * -- GETTER --
     *  Get the underlying MongoDB client session for this transaction.
     *  This can be used by MongoDB operations to ensure they participate in the transaction.
     */
    @Getter
    private final ClientSession clientSession;
    private final TransactionOptions transactionOptions;
    private boolean active = false;
    @Getter
    private boolean rolledBack = false;
    @Getter
    private boolean committed = false;
    private boolean rollbackOnly = false;
    
    public MongoTransaction(MongoClient mongoClient) {
        this.mongoClient = mongoClient;
        this.transactionOptions = TransactionOptions.builder().build();
        this.clientSession = mongoClient.startSession();
        log.debug("Created new MongoDB transaction");
    }
    
    public MongoTransaction(MongoClient mongoClient, TransactionOptions options) {
        this.mongoClient = mongoClient;
        this.transactionOptions = options;
        this.clientSession = mongoClient.startSession();
        log.debug("Created new MongoDB transaction with options: {}", options);
    }
    
    public void begin() {
        if (active) {
            throw new IllegalStateException("Transaction is already active");
        }
        
        try {
            clientSession.startTransaction(transactionOptions);
            active = true;
            log.debug("Started MongoDB transaction");
        } catch (Exception e) {
            log.error("Failed to start MongoDB transaction", e);
            throw new HeliosException("Failed to start transaction: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void commit() {
        if (!active) {
            throw new IllegalStateException("No active transaction to commit");
        }
        
        if (rolledBack) {
            throw new IllegalStateException("Transaction has been rolled back, cannot commit");
        }
        
        if (committed) {
            throw new IllegalStateException("Transaction has already been committed");
        }
        
        try {
            clientSession.commitTransaction();
            committed = true;
            active = false;
            log.debug("Committed MongoDB transaction");
        } catch (Exception e) {
            log.error("Failed to commit MongoDB transaction", e);
            throw new HeliosException("Failed to commit transaction: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void rollback() {
        if (!active) {
            log.warn("Attempting to rollback inactive transaction");
            return;
        }
        
        if (committed) {
            throw new IllegalStateException("Transaction has already been committed, cannot rollback");
        }
        
        try {
            clientSession.abortTransaction();
            rolledBack = true;
            active = false;
            log.debug("Rolled back MongoDB transaction");
        } catch (Exception e) {
            log.error("Failed to rollback MongoDB transaction", e);
            throw new HeliosException("Failed to rollback transaction: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean isActive() {
        return active && clientSession.hasActiveTransaction();
    }

    @Override
    public boolean isRollbackOnly() {
        return rollbackOnly;
    }
    
    @Override
    public void setRollbackOnly() {
        if (!active) {
            throw new IllegalStateException("Cannot mark inactive transaction for rollback");
        }
        this.rollbackOnly = true;
        log.debug("Transaction marked for rollback only");
    }
    
    @Override
    public void close() {
        try {
            if (isActive()) {
                log.warn("Closing transaction that is still active, rolling back");
                rollback();
            }
        } catch (Exception e) {
            log.error("Error during transaction rollback on close", e);
        } finally {
            try {
                clientSession.close();
                log.debug("Closed MongoDB transaction session");
            } catch (Exception e) {
                log.error("Error closing MongoDB session", e);
            }
        }
    }

}