package lib.concurrent;


import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * CompletableFuture ve CancellableFuture zincirlerini Android ana (UI) thread’inde koşturmak için yardımcılar.
 * Not: CancellableFuture OVERLOAD'larını kullanırsanız tür korunur; iptal yayılımı da sürer.
 */
public final class AndroidFutures {

    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    /**
     * Tekil Main Thread executor.
     */
    public static final Executor MAIN = r -> {
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) r.run();
        else MAIN_HANDLER.post(r);
    };

    private AndroidFutures() {
    }

    /* ===================== Common helpers ===================== */

    /**
     * UI’da çalıştır.
     */
    public static void runOnMain(Runnable r) {
        MAIN.execute(r);
    }

    /* ===================== CompletableFuture variants ===================== */

    public static <T> CompletableFuture<Void> thenAcceptOnMain(
            CompletableFuture<? extends T> f, Consumer<? super T> action) {
        return f.thenAcceptAsync(action, MAIN);
    }

    public static <T, R> CompletableFuture<R> thenApplyOnMain(
            CompletableFuture<? extends T> f, Function<? super T, ? extends R> fn) {
        return f.thenApplyAsync(fn, MAIN);
    }

    public static <T> CompletableFuture<T> whenCompleteOnMain(
            CompletableFuture<T> f, BiConsumer<? super T, ? super Throwable> action) {
        return f.whenCompleteAsync(action, MAIN);
    }

    /**
     * exceptionallyAsync (Java 21) yerine handleAsync ile uyumlu çözüm.
     */
    public static <T> CompletableFuture<T> exceptionallyOnMain(
            CompletableFuture<T> f, Function<Throwable, ? extends T> fn) {
        return f.handleAsync((v, e) -> e == null ? v : fn.apply(unwrap(e)), MAIN);
    }

    /* ===================== CancellableFuture variants (type-preserving) ===================== */

    public static <T> CancellableFuture<Void> thenAcceptOnMain(
            CancellableFuture<? extends T> f, Consumer<? super T> action) {
        CancellableFuture<Void> next = new CancellableFuture<>();
        next.inheritCancellationFrom(f);
        f.whenComplete((v, e) -> runOnMain(() -> {
            if (f.isWorkCancelled()) {
                next.cancel(true);
                return;
            }
            if (e != null) {
                next.completeExceptionally(e);
                return;
            }
            try {
                action.accept(v);
                next.complete(null);
            } catch (Throwable t) {
                next.completeExceptionally(t);
            }
        }));
        // Downstream iptali upstream'e de taşı
        next.whenComplete((vv, ee) -> {
            if (next.isCancelled() && !f.isCancelled()) try {
                f.cancel(true);
            } catch (Throwable ignored) {
            }
        });
        return next;
    }

    public static <T, R> CancellableFuture<R> thenApplyOnMain(
            CancellableFuture<? extends T> f, Function<? super T, ? extends R> fn) {
        CancellableFuture<R> next = new CancellableFuture<>();
        next.inheritCancellationFrom(f);
        f.whenComplete((v, e) -> runOnMain(() -> {
            if (f.isWorkCancelled()) {
                next.cancel(true);
                return;
            }
            if (e != null) {
                next.completeExceptionally(e);
                return;
            }
            try {
                next.complete(fn.apply(v));
            } catch (Throwable t) {
                next.completeExceptionally(t);
            }
        }));
        next.whenComplete((vv, ee) -> {
            if (next.isCancelled() && !f.isCancelled()) try {
                f.cancel(true);
            } catch (Throwable ignored) {
            }
        });
        return next;
    }

    public static <T, R> CancellableFuture<R> thenComposeOnMain(
            CancellableFuture<? extends T> f, Function<? super T, ? extends CancellableFuture<R>> fn) {
        CancellableFuture<R> next = new CancellableFuture<>();
        next.inheritCancellationFrom(f);
        f.whenComplete((v, e) -> runOnMain(() -> {
            if (f.isWorkCancelled()) {
                next.cancel(true);
                return;
            }
            if (e != null) {
                next.completeExceptionally(e);
                return;
            }
            try {
                CancellableFuture<R> inner = fn.apply(v);
                if (inner == null) {
                    next.complete(null);
                    return;
                }
                next.inheritCancellationFrom(inner);
                inner.whenComplete((u, ex) -> {
                    if (inner.isWorkCancelled()) next.cancel(true);
                    else if (ex != null) next.completeExceptionally(ex);
                    else next.complete(u);
                });
                // Downstream iptali inner ve upstream'e taşı
                next.whenComplete((uu, ex2) -> {
                    if (next.isCancelled()) {
                        try {
                            if (!inner.isCancelled()) inner.cancel(true);
                        } catch (Throwable ignored) {
                        }
                        try {
                            if (!f.isCancelled()) f.cancel(true);
                        } catch (Throwable ignored) {
                        }
                    }
                });
            } catch (Throwable t) {
                next.completeExceptionally(t);
            }
        }));
        return next;
    }

    public static <T> CancellableFuture<T> whenCompleteOnMain(
            CancellableFuture<T> f, BiConsumer<? super T, ? super Throwable> action) {
        CancellableFuture<T> next = new CancellableFuture<>();
        next.inheritCancellationFrom(f);
        f.whenComplete((v, e) -> runOnMain(() -> {
            if (f.isWorkCancelled()) {
                next.cancel(true);
                return;
            }
            try {
                action.accept(v, e);
            } catch (Throwable t) {
                next.completeExceptionally(t);
                return;
            }
            if (e != null) next.completeExceptionally(e);
            else next.complete(v);
        }));
        next.whenComplete((vv, ee) -> {
            if (next.isCancelled() && !f.isCancelled()) try {
                f.cancel(true);
            } catch (Throwable ignored) {
            }
        });
        return next;
    }

    public static <T> CancellableFuture<T> exceptionallyOnMain(
            CancellableFuture<T> f, Function<Throwable, ? extends T> fn) {
        CancellableFuture<T> next = new CancellableFuture<>();
        next.inheritCancellationFrom(f);
        f.whenComplete((v, e) -> runOnMain(() -> {
            if (f.isWorkCancelled()) {
                next.cancel(true);
                return;
            }
            if (e == null) {
                next.complete(v);
                return;
            }
            try {
                next.complete(fn.apply(unwrap(e)));
            } catch (Throwable t) {
                next.completeExceptionally(t);
            }
        }));
        next.whenComplete((vv, ee) -> {
            if (next.isCancelled() && !f.isCancelled()) try {
                f.cancel(true);
            } catch (Throwable ignored) {
            }
        });
        return next;
    }

    /* ===================== utils ===================== */
    private static Throwable unwrap(Throwable e) {
        return (e instanceof CompletionException && e.getCause() != null) ? e.getCause() : e;
    }
}
