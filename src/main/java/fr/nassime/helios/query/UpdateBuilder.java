package fr.nassime.helios.query;

import java.util.Map;

public class UpdateBuilder extends QueryBuilder<UpdateBuilder> {

    private UpdateBuilder() {
        super();
        query.append("UPDATE ");
    }

    public static UpdateBuilder create() {
        return new UpdateBuilder();
    }

    public UpdateBuilder table(String tableName) {
        query.append(tableName);
        return this;
    }

    public UpdateBuilder set(Map<String, Object> columnValues) {
        if (columnValues == null || columnValues.isEmpty()) {
            throw new IllegalArgumentException("The column and value map cannot be empty");
        }

        query.append(" SET ");
        int i = 0;
        for (Map.Entry<String, Object> entry : columnValues.entrySet()) {
            if (i > 0) {
                query.append(", ");
            }

            query.append(entry.getKey()).append(" = ?");
            parameters.add(entry.getValue());

            i++;
        }

        return this;
    }

    public UpdateBuilder returning(String... columns) {
        query.append(" RETURNING ").append(String.join(", ", columns));
        return this;
    }
}

