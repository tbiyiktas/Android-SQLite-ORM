package lib.persistence;

/** DbContext davranışını merkezi olarak ayarlamak için konfig. */
public final class DbContextConfig {

    // --- Threading ---
    /** Okuma havuzu thread sayısı (>=1) */
    public int readThreads = 4;
    /** Okuma thread ad prefix'i (log/diagnostic) */
    public String readThreadNamePrefix = "db-read-";
    /** Yazma thread adı */
    public String writeThreadName = "db-write-1";

    // --- SQLite/WAL/PRAGMA ---
    /** Write-Ahead Logging */
    public boolean enableWAL = true;
    /** PRAGMA journal_mode (WAL, TRUNCATE, DELETE...) */
    public String pragmaJournalMode = "WAL";
    /** PRAGMA synchronous (OFF, NORMAL, FULL, EXTRA) */
    public String pragmaSynchronous = "NORMAL";
    /** PRAGMA foreign_keys */
    public boolean pragmaForeignKeys = true;
    /** PRAGMA busy_timeout (ms); <=0 ise uygulanmaz */
    public int pragmaBusyTimeoutMs = 10000;

    // --- Global erişim (lazy) ---
    private static volatile DbContextConfig GLOBAL = new DbContextConfig();

    private DbContextConfig() {}

    /** Global config'i al. */
    public static DbContextConfig get() { return GLOBAL; }

    /** Global config'i uygula. Uygulama açılışında çağır. */
    public static void apply(DbContextConfig cfg) {
        if (cfg == null) throw new IllegalArgumentException("cfg null olamaz");
        GLOBAL = cfg;
    }
}
