package lib.persistence.converters;

// F: model alan tipi (EventType, Date, vs.)
// D: DB'ye yazılacak temel tip (String, Long, Double, byte[])
public interface TypeConverter<F, D> {
    D toDatabaseValue(F fieldValue);
    F fromDatabaseValue(D databaseValue);

    // Bu converter hangi SQLite tipini kullanır? "TEXT" | "INTEGER" | "REAL" | "BLOB"
    default String sqliteType() { return "TEXT"; }
}
