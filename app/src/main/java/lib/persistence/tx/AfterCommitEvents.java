// lib/persistence/tx/AfterCommitEvents.java
package lib.persistence.tx;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class AfterCommitEvents {
//    private static final ThreadLocal<List<Object>> OUTBOX = ThreadLocal.withInitial(ArrayList::new);
//    private static final Map<Class<?>, Consumer<Object>> HANDLERS = new HashMap<>();

    private static final ThreadLocal<List<Object>> OUTBOX = ThreadLocal.withInitial(ArrayList::new);
    private static final java.util.concurrent.ConcurrentHashMap<Class<?>, java.util.concurrent.CopyOnWriteArrayList<Consumer<Object>>> HANDLERS = new java.util.concurrent.ConcurrentHashMap<>();

    private AfterCommitEvents() {
    }

    public static <T> void add(T ev) {
        OUTBOX.get().add(ev);
    }

    @SuppressWarnings("unchecked")
//    public static <T> void register(Class<T> type, java.util.function.Consumer<T> handler) {
//        HANDLERS.put(type, (Consumer<Object>) handler);
//    }
    public static <T> void register(Class<T> type, java.util.function.Consumer<T> handler) {
        HANDLERS.computeIfAbsent(type, k -> new java.util.concurrent.CopyOnWriteArrayList<>())
                .add((Consumer<Object>) handler);
    }

    /**
     * Commit sonrası çağrılır.
     */
    public static void publishAndClear() {
        List<Object> box = OUTBOX.get();
        try {
//            for (Object e : box) {
//                Consumer<Object> h = HANDLERS.get(e.getClass());
//                if (h != null) h.accept(e);
//            }
            for (Object e : box) {
                var list = HANDLERS.get(e.getClass());
                if (list != null) for (var h : list) h.accept(e);
            }
        } finally {
            box.clear();
        }
    }

    /**
     * Rollback/failure durumunda çağır.
     */
    public static void clear() {
        OUTBOX.get().clear();
    }
}
