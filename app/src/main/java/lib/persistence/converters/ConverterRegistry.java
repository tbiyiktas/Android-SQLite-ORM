package lib.persistence.converters;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ConverterRegistry {
    private static final Map<Class<?>, Object> INSTANCES = new ConcurrentHashMap<>();
    private ConverterRegistry() {}

    @SuppressWarnings("unchecked")
    public static <C> C getOrCreate(Class<C> type) {
        return (C) INSTANCES.computeIfAbsent(type, ConverterRegistry::newInstance);
    }
    private static Object newInstance(Class<?> c) {
        try { return c.getDeclaredConstructor().newInstance(); }
        catch (Exception e) { throw new IllegalStateException("Cannot instantiate converter: " + c, e); }
    }
}
