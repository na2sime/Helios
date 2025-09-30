package fr.nassime.helios.api.transaction;

/**
 * Represents a database transaction.
 */
public interface Transaction {
    
    /**
     * Commit the transaction.
     */
    void commit();
    
    /**
     * Rollback the transaction.
     */
    void rollback();
    
    /**
     * Check if the transaction is active.
     */
    boolean isActive();
    
    /**
     * Check if the transaction is marked for rollback only.
     */
    boolean isRollbackOnly();
    
    /**
     * Mark the transaction for rollback only.
     */
    void setRollbackOnly();
}