package fr.nassime.helios.query;

import java.util.List;
import java.util.Map;

public class InsertBuilder extends QueryBuilder<InsertBuilder> {

    private InsertBuilder() {
        super();
        query.append("INSERT INTO ");
    }

    public static InsertBuilder create() {
        return new InsertBuilder();
    }

    public InsertBuilder into(String tableName) {
        query.append(tableName);
        return this;
    }

    public InsertBuilder values(Map<String, Object> columnValues) {
        if (columnValues == null || columnValues.isEmpty()) {
            throw new IllegalArgumentException("The column and value map cannot be empty");
        }

        List<String> columns = columnValues.keySet().stream().toList();
        query.append(" (").append(String.join(", ", columns)).append(")");

        query.append(" VALUES (");
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) {
                query.append(", ");
            }
            query.append("?");
            parameters.add(columnValues.get(columns.get(i)));
        }
        query.append(")");

        return this;
    }

    public InsertBuilder returning(String... columns) {
        query.append(" RETURNING ").append(String.join(", ", columns));
        return this;
    }
}

