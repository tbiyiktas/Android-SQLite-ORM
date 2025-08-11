package lib.persistence.profile;

public enum DbDataType {
    // Metin verileri için
    TEXT,

    // Tamsayılar, boolean (0 veya 1) ve tarih/saat verileri için
    INTEGER,

    // Ondalıklı sayılar için
    REAL,

    // Binary veriler (resim, dosya) için
    BLOB,

    // Sayısal değerler için (REAL ile benzer)
    NUMERIC
}
