package fr.nassime.helios.query;

import fr.nassime.helios.exception.HeliosException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public abstract class QueryBuilder<T extends QueryBuilder<T>> {

    protected final StringBuilder query;
    @Getter
    protected final List<Object> parameters;

    protected QueryBuilder() {
        this.query = new StringBuilder();
        this.parameters = new ArrayList<>();
    }

    public String getQuery() {
        return query.toString();
    }

    public PreparedStatement prepareStatement(Connection connection) {
        try {
            log.debug("Preparing SQL query: {}", getQuery());
            PreparedStatement statement = connection.prepareStatement(getQuery());
            setParameters(statement);
            return statement;
        } catch (SQLException e) {
            throw new HeliosException("Error while preparing the query", e);
        }
    }

    public PreparedStatement prepareStatementWithGeneratedKeys(Connection connection) {
        try {
            log.debug("Preparing SQL query with generated keys: {}", getQuery());
            PreparedStatement statement = connection.prepareStatement(getQuery(),
                    PreparedStatement.RETURN_GENERATED_KEYS);
            setParameters(statement);
            return statement;
        } catch (SQLException e) {
            throw new HeliosException("Error while preparing the query with generated keys", e);
        }
    }

    protected void setParameters(PreparedStatement statement) throws SQLException {
        for (int i = 0; i < parameters.size(); i++) {
            Object param = parameters.get(i);
            setParameter(statement, i + 1, param);
        }
    }

    protected void setParameter(PreparedStatement statement, int index, Object value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.NULL);
        } else if (value instanceof LocalDateTime) {
            statement.setTimestamp(index, Timestamp.valueOf((LocalDateTime) value));
        } else if (value instanceof LocalDate) {
            statement.setDate(index, java.sql.Date.valueOf((LocalDate) value));
        } else if (value instanceof Enum) {
            statement.setString(index, ((Enum<?>) value).name());
        } else {
            statement.setObject(index, value);
        }
    }

    @SuppressWarnings("unchecked")
    public T where(Map<String, Object> conditions) {
        if (conditions != null && !conditions.isEmpty()) {
            query.append(" WHERE ");
            int i = 0;
            for (Map.Entry<String, Object> entry : conditions.entrySet()) {
                if (i > 0) {
                    query.append(" AND ");
                }

                if (entry.getValue() == null) {
                    query.append(entry.getKey()).append(" IS NULL");
                } else {
                    query.append(entry.getKey()).append(" = ?");
                    parameters.add(entry.getValue());
                }
                i++;
            }
        }
        return (T) this;
    }
}
