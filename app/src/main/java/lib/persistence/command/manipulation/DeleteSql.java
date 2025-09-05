package lib.persistence.command.manipulation;

import static lib.persistence.SqlNames.qCol;

import java.util.ArrayList;
import java.util.List;

import lib.persistence.annotations.DbTableAnnotation;

public class DeleteSql {
    private final Class<?> type;
    private final String tableName;
    private final List<String> whereClauses = new ArrayList<>();
    private final List<String> whereArgs = new ArrayList<>();
    private String pendingOp = "AND"; // and()/or() ile güncellenir

    private DeleteSql(Class<?> type) {
        this.type = type;
        DbTableAnnotation ann = type.getAnnotation(DbTableAnnotation.class);
        this.tableName = (ann != null && !ann.name().isEmpty()) ? ann.name() : type.getSimpleName();
    }

    public static DeleteSql build(Class<?> type) {
        if (type == null) throw new IllegalArgumentException("type boş olamaz");
        return new DeleteSql(type);
    }

    /** Okunabilirlik için; işlevsel değişiklik yapmaz. */
    public DeleteSql where() { return this; }

    /** Sonraki koşulun başına AND ekler. */
    public DeleteSql and() { this.pendingOp = "AND"; return this; }

    /** Sonraki koşulun başına OR ekler. */
    public DeleteSql or() { this.pendingOp = "OR"; return this; }

    /** column = ? */
    public DeleteSql Equals(String column, Object value) {
        if (column == null || column.trim().isEmpty())
            throw new IllegalArgumentException("column zorunludur");

        String clause =  qCol(column.trim()) + " = ?";
        if (!whereClauses.isEmpty()) {
            clause = pendingOp + " " + clause; // önceki koşullara bağla
        }
        whereClauses.add(clause);
        whereArgs.add(String.valueOf(value));
        // her eklemeden sonra varsayılanı AND yap
        this.pendingOp = "AND";
        return this;
    }

    // İhtiyaca göre diğer operatörler (NotEquals, GreaterThan, Like, IsNull, vb.) eklenebilir.

    public String getTableName() {
        return tableName;
    }

    public String getWhereClause() {
        if (whereClauses.isEmpty()) return null; // WHERE yok → tüm satırlar
        // whereClauses içinde op'lu parçalar mevcut: "col=?"/"AND col=?"/"OR col=?"
        // Baştaki parça op'suz; birleşim:
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < whereClauses.size(); i++) {
            String part = whereClauses.get(i);
            if (i == 0) {
                // ilk parça op'suz gelmişse doğrudan eklenir
                sb.append(part.startsWith("AND ") || part.startsWith("OR ") ? part.substring(4) : part);
            } else {
                sb.append(" ").append(part);
            }
        }
        return sb.toString();
    }

    public String[] getWhereArgs() {
        return whereArgs.toArray(new String[0]);
    }
}
