// lib/persistence/DbContextConfig.java
package lib.persistence;

import java.util.concurrent.atomic.AtomicInteger;

/** Global DB ayarları (threading + PRAGMA). Uygulama açılışında apply(...) ile set edin. */
public final class DbContextConfig {

    // Threading
    public final int readThreads;
    public final int readQueueCapacity;
    public final int writeQueueCapacity;
    public final boolean nonExclusiveTransactions;
    public final String readThreadNamePrefix;
    public final String writeThreadName;

    // PRAGMA / bağlantı
    public final boolean enableWAL;
    public final boolean pragmaForeignKeys;
    public final String  pragmaJournalMode;   // "WAL"
    public final String  pragmaSynchronous;   // "NORMAL" / "FULL" / ...
    public final int     pragmaBusyTimeoutMs; // 0 => kapalı

    // Read thread adları için sayaç
    private final AtomicInteger readIndex = new AtomicInteger(1);
    public int nextReadIndex() { return readIndex.getAndIncrement(); }

    // ---- Singleton ----
    private static volatile DbContextConfig INSTANCE = defaults();
    public static DbContextConfig get() { return INSTANCE; }
    public static void apply(DbContextConfig cfg) { INSTANCE = cfg; }
    public static Builder builder() { return new Builder(INSTANCE); }

    private DbContextConfig(Builder b) {
        this.readThreads = b.readThreads;
        this.readQueueCapacity = b.readQueueCapacity;
        this.writeQueueCapacity = b.writeQueueCapacity;
        this.nonExclusiveTransactions = b.nonExclusiveTransactions;
        this.readThreadNamePrefix = b.readThreadNamePrefix;
        this.writeThreadName = b.writeThreadName;

        this.enableWAL = b.enableWAL;
        this.pragmaForeignKeys = b.pragmaForeignKeys;
        this.pragmaJournalMode = b.pragmaJournalMode;
        this.pragmaSynchronous = b.pragmaSynchronous;
        this.pragmaBusyTimeoutMs = b.pragmaBusyTimeoutMs;
    }

    private static DbContextConfig defaults() {
        return builder()
                .readThreads(4)
                .readQueueCapacity(256)
                .writeQueueCapacity(1024)
                .nonExclusiveTransactions(true)
                .readThreadNamePrefix("db-read-")
                .writeThreadName("db-write")
                .enableWAL(true)
                .pragmaForeignKeys(true)
                .pragmaJournalMode("WAL")
                .pragmaSynchronous("NORMAL")
                .pragmaBusyTimeoutMs(10_000)
                .build();
    }

    // ---- Builder ----
    public static final class Builder {
        private int readThreads = 4;
        private int readQueueCapacity = 256;
        private int writeQueueCapacity = 1024;
        private boolean nonExclusiveTransactions = true;
        private String readThreadNamePrefix = "DB-Read-";
        private String writeThreadName = "DB-Write";

        private boolean enableWAL = true;
        private boolean pragmaForeignKeys = true;
        private String  pragmaJournalMode = "WAL";
        private String  pragmaSynchronous = "NORMAL";
        private int     pragmaBusyTimeoutMs = 10_000;

        private Builder(DbContextConfig from) {
            if (from != null) {
                this.readThreads = from.readThreads;
                this.readQueueCapacity = from.readQueueCapacity;
                this.writeQueueCapacity = from.writeQueueCapacity;
                this.nonExclusiveTransactions = from.nonExclusiveTransactions;
                this.readThreadNamePrefix = from.readThreadNamePrefix;
                this.writeThreadName = from.writeThreadName;

                this.enableWAL = from.enableWAL;
                this.pragmaForeignKeys = from.pragmaForeignKeys;
                this.pragmaJournalMode = from.pragmaJournalMode;
                this.pragmaSynchronous = from.pragmaSynchronous;
                this.pragmaBusyTimeoutMs = from.pragmaBusyTimeoutMs;
            }
        }

        public Builder readThreads(int v) { this.readThreads = v; return this; }
        public Builder readQueueCapacity(int v) { this.readQueueCapacity = v; return this; }
        public Builder writeQueueCapacity(int v) { this.writeQueueCapacity = v; return this; }
        public Builder nonExclusiveTransactions(boolean v) { this.nonExclusiveTransactions = v; return this; }
        public Builder readThreadNamePrefix(String v) { this.readThreadNamePrefix = v; return this; }
        public Builder writeThreadName(String v) { this.writeThreadName = v; return this; }

        public Builder enableWAL(boolean v) { this.enableWAL = v; return this; }
        public Builder pragmaForeignKeys(boolean v) { this.pragmaForeignKeys = v; return this; }
        public Builder pragmaJournalMode(String v) { this.pragmaJournalMode = v; return this; }
        public Builder pragmaSynchronous(String v) { this.pragmaSynchronous = v; return this; }
        public Builder pragmaBusyTimeoutMs(int v) { this.pragmaBusyTimeoutMs = v; return this; }

        public DbContextConfig build() { return new DbContextConfig(this); }
    }
}
