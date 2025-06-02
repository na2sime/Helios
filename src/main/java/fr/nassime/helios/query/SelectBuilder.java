package fr.nassime.helios.query;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SelectBuilder extends QueryBuilder<SelectBuilder> {

    private SelectBuilder() {
        super();
        query.append("SELECT ");
    }

    public static SelectBuilder create() {
        return new SelectBuilder();
    }

    public SelectBuilder columns(String... columns) {
        return columns(Arrays.asList(columns));
    }

    public SelectBuilder columns(List<String> columns) {
        if (columns == null || columns.isEmpty()) {
            query.append("*");
        } else {
            query.append(String.join(", ", columns));
        }
        return this;
    }

    public SelectBuilder from(String tableName) {
        query.append(" FROM ").append(tableName);
        return this;
    }

    public SelectBuilder orderBy(String column, boolean ascending) {
        query.append(" ORDER BY ").append(column);
        if (!ascending) {
            query.append(" DESC");
        }
        return this;
    }

    public SelectBuilder limit(int limit) {
        query.append(" LIMIT ?");
        parameters.add(limit);
        return this;
    }

    public SelectBuilder offset(int offset) {
        query.append(" OFFSET ?");
        parameters.add(offset);
        return this;
    }

    public SelectBuilder groupBy(String... columns) {
        query.append(" GROUP BY ").append(String.join(", ", columns));
        return this;
    }

    public SelectBuilder having(String condition, Object... params) {
        query.append(" HAVING ").append(condition);
        Collections.addAll(parameters, params);
        return this;
    }

    public SelectBuilder whereCustom(String condition, Object... params) {
        query.append(" WHERE ").append(condition);
        Collections.addAll(parameters, params);
        return this;
    }

    public SelectBuilder join(String joinType, String table, String on) {
        query.append(" ").append(joinType).append(" JOIN ").append(table).append(" ON ").append(on);
        return this;
    }

    public SelectBuilder innerJoin(String table, String on) {
        return join("INNER", table, on);
    }

    public SelectBuilder leftJoin(String table, String on) {
        return join("LEFT", table, on);
    }
}