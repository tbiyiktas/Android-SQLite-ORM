// app/src/main/java/com/example/adbkit/events/SimpleEventBus.java
package com.example.adbkit.events;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Basit, süreç-içi (in-process) event bus:
 * - Bir event türüne birden çok subscriber (fan-out)
 * - İsteğe bağlı "main thread" teslimi
 * - Abonelik kaldırma ve tümünü temizleme
 * - Post çağrısı senkron; main teslimi gerekiyorsa Handler ile post edilir.
 */
public final class SimpleEventBus {
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final Map<Class<?>, CopyOnWriteArrayList<Subscriber>> SUBS = new ConcurrentHashMap<>();
    private SimpleEventBus() {
    }

    private static boolean isMainThread() {
        return Thread.currentThread() == Looper.getMainLooper().getThread();
    }

    /**
     * Geçerli thread’de teslim.
     */
    public static <T> Subscription subscribe(@NonNull Class<T> eventType,
                                             @NonNull Consumer<T> listener) {
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(listener, "listener");
        SUBS.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                .add(new Subscriber(listener, /*deliverOnMain=*/false));
        return new Subscription(eventType, listener);
    }

    /**
     * Ana thread’de teslim (UI güncellemeleri için).
     */
    public static <T> Subscription subscribeOnMain(@NonNull Class<T> eventType,
                                                   @NonNull Consumer<T> listener) {
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(listener, "listener");
        SUBS.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                .add(new Subscriber(listener, /*deliverOnMain=*/true));
        return new Subscription(eventType, listener);
    }

    /**
     * Aboneliği kaldır.
     */
    public static <T> void unsubscribe(@NonNull Class<?> eventType,
                                       @NonNull Consumer<?> listener) {
        var list = SUBS.get(eventType);
        if (list == null) return;
        list.removeIf(s -> s.matches(listener));
        if (list.isEmpty()) SUBS.remove(eventType);
    }

    /**
     * Tüm abonelikleri temizle (örn. testlerde).
     */
    public static void clearAll() {
        SUBS.clear();
    }

    /**
     * Event’i yayınla (senkron). Gerekirse main thread’e post eder.
     */
    @SuppressWarnings("unchecked")
    public static <T> void post(@NonNull T event) {
        Objects.requireNonNull(event, "event");
        var list = SUBS.get(event.getClass());
        if (list == null || list.isEmpty()) return;

        for (var sub : list) {
            if (sub.deliverOnMain && !isMainThread()) {
                MAIN.post(() -> safeAccept((Consumer<T>) sub.consumer, event));
            } else {
                safeAccept((Consumer<T>) sub.consumer, event);
            }
        }
    }

    private static <T> void safeAccept(Consumer<T> c, T e) {
        try {
            c.accept(e);
        } catch (Throwable ignored) { /* izole et */ }
    }

    // --- helpers ---

    /**
     * Abonelik için elde tutabileceğin küçük bir token. close() => unsubscribe.
     */
    public static final class Subscription implements AutoCloseable {
        private final Class<?> eventType;
        private final Consumer<?> consumer;

        private Subscription(Class<?> type, Consumer<?> consumer) {
            this.eventType = type;
            this.consumer = consumer;
        }

        @Override
        public void close() {
            unsubscribe(eventType, consumer);
        }
    }

    private static final class Subscriber {
        final Consumer<?> consumer;
        final boolean deliverOnMain;

        Subscriber(Consumer<?> consumer, boolean deliverOnMain) {
            this.consumer = consumer;
            this.deliverOnMain = deliverOnMain;
        }

        @SuppressWarnings("unchecked")
        boolean matches(Consumer<?> other) {
            return this.consumer == other;
        }
    }
}
