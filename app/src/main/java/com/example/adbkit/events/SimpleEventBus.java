package com.example.adbkit.events;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class SimpleEventBus {
    private static final Map<Class<?>, Consumer<?>> subscribers = new ConcurrentHashMap<>();

    // Olay aboneliği
    @SuppressWarnings("unchecked")
    public static <T> void subscribe(Class<T> eventType, Consumer<T> listener) {
        subscribers.put(eventType, listener);
    }

    // Olay yayınlama
    @SuppressWarnings("unchecked")
    public static <T> void post(T event) {
        Consumer<T> listener = (Consumer<T>) subscribers.get(event.getClass());
        if (listener != null) {
            listener.accept(event);
        }
    }
}