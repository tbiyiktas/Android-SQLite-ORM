package lib.persistence.profile;

public enum DbDataType {
    // Tamsayılar, boolean (0 veya 1) ve tarih/saat verileri için
    INTEGER,

    // Ondalıklı sayılar için
    REAL,

    // Metin verileri için
    TEXT,

    // Binary veriler (resim, dosya) için
    BLOB,
}
