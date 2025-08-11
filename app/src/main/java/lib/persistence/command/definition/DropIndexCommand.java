package lib.persistence.command.definition;

public class DropIndexCommand {
    private final String query;

    private DropIndexCommand(String query) {
        this.query = query;
    }

    public static DropIndexCommand build(String indexName) {
        return new DropIndexCommand("DROP INDEX IF EXISTS " + indexName);
    }

    public String getQuery() {
        return query;
    }
}