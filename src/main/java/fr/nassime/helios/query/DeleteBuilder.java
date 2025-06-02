package fr.nassime.helios.query;

public class DeleteBuilder extends QueryBuilder<DeleteBuilder> {

    private DeleteBuilder() {
        super();
        query.append("DELETE FROM ");
    }

    public static DeleteBuilder create() {
        return new DeleteBuilder();
    }

    public DeleteBuilder from(String tableName) {
        query.append(tableName);
        return this;
    }

    public DeleteBuilder returning(String... columns) {
        query.append(" RETURNING ").append(String.join(", ", columns));
        return this;
    }
}

