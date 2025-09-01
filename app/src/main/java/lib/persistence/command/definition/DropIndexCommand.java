package lib.persistence.command.definition;

public class DropIndexCommand {

    private final String query;

    private DropIndexCommand(String query) {
        this.query = query;
    }

    public static DropIndexCommand build(String indexName) {
        if (indexName == null || indexName.trim().isEmpty())
            throw new IllegalArgumentException("indexName zorunludur");
        String q = "DROP INDEX IF EXISTS " + safeId(indexName) + ";";
        return new DropIndexCommand(q);
    }

    private static String safeId(String id) {
        String trimmed = id.trim();
        if (!trimmed.matches("[A-Za-z_][A-Za-z0-9_]*"))
            throw new IllegalArgumentException("Geçersiz identifier: " + id);
        return trimmed;
    }

    public String getQuery() { return query; }
}
