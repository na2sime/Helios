package fr.nassime.helios.api.query;

/**
 * Defines query operators for building conditions.
 */
public enum QueryOperator {
    EQUALS("="),
    NOT_EQUALS("<>"),
    GREATER_THAN(">"),
    GREATER_THAN_OR_EQUAL(">="),
    LESS_THAN("<"),
    LESS_THAN_OR_EQUAL("<="),
    LIKE("LIKE"),
    NOT_LIKE("NOT LIKE"),
    IN("IN"),
    NOT_IN("NOT IN"),
    IS_NULL("IS NULL"),
    IS_NOT_NULL("IS NOT NULL"),
    BETWEEN("BETWEEN"),
    NOT_BETWEEN("NOT BETWEEN");
    
    private final String sql;
    
    QueryOperator(String sql) {
        this.sql = sql;
    }
    
    public String getSql() {
        return sql;
    }
}