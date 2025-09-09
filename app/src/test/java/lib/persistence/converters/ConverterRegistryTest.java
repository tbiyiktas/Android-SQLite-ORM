package lib.persistence.converters;

import org.junit.Test;
import static org.junit.Assert.*;

import lib.persistence.mapping.converters.ConverterRegistry;
import lib.persistence.mapping.converters.TypeConverter;

public class ConverterRegistryTest {

    public static class Dummy implements TypeConverter<String,String> {
        public Dummy() {}
        @Override public String toDatabaseValue(String v) { return v; }
        @Override public String fromDatabaseValue(String d) { return d; }
    }

    @Test public void getOrCreate_isSingletonPerClass() {
        Dummy c1 = ConverterRegistry.getOrCreate(Dummy.class);
        Dummy c2 = ConverterRegistry.getOrCreate(Dummy.class);
        assertSame(c1, c2);
    }
}
