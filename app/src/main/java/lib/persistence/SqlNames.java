package lib.persistence;

public final class SqlNames {
    private SqlNames() {}
    public static String qId(String id) {
        String s = id.trim();
        if (s.contains("(") || s.contains(" ") || s.contains("`") || s.contains("\"")) return s;
        return "`" + s + "`";
    }
    public static String qCol(String col) {
        String c = col.trim();
        if ("*".equals(c)) return "*";
        int dot = c.indexOf('.');
        return (dot>0 && dot<c.length()-1) ? qId(c.substring(0,dot)) + "." + qId(c.substring(dot+1)) : qId(c);
    }


    /** SQLite identifier için basit güvenlik filtresi (harf/rakam/_). İstersen kaldır. */
    public static String safeId(String id) {
        String trimmed = id.trim();
        if (!trimmed.matches("[A-Za-z_][A-Za-z0-9_]*"))
            throw new IllegalArgumentException("Geçersiz identifier: " + id);
        return trimmed;
    }
}
