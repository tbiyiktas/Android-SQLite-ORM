package lib.persistence.command.definition;

import static lib.persistence.SqlNames.qId;

public class DropIndexCommand {

    private final String query;

    private DropIndexCommand(String query) {
        this.query = query;
    }

    public static DropIndexCommand build(String indexName) {
        if (indexName == null || indexName.trim().isEmpty())
            throw new IllegalArgumentException("indexName zorunludur");
        String q = "DROP INDEX IF EXISTS " + qId(indexName) + ";";
        return new DropIndexCommand(q);
    }

    public String getQuery() { return query; }
}
