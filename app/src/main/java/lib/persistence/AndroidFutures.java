package lib.persistence;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import lib.persistence.core.CancellableFuture;

/**
 * CompletableFuture/CancellableFuture zincirlerini Android ana (UI) thread’inde koşturmak için yardımcılar.
 */
public final class AndroidFutures {

    private AndroidFutures() {
    }

    /**
     * Tekil Main Thread executor.
     */
//    public static final Executor MAIN = runnable -> {
//        if (Looper.getMainLooper().isCurrentThread()) {
//            runnable.run();
//        } else {
//            new Handler(Looper.getMainLooper()).post(runnable);
//        }
//    };

    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    public static final Executor MAIN = r -> {
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) r.run();
        else MAIN_HANDLER.post(r);
    };

    /**
     * Basitçe UI’da çalıştır.
     */
    public static void runOnMain(Runnable r) {
        MAIN.execute(r);
    }

    // ---------- thenAccept / thenApply / whenComplete / exceptionally varyantları ----------

    public static <T> CompletableFuture<Void> thenAcceptOnMain(CompletableFuture<T> f, Consumer<T> action) {
        return f.thenAcceptAsync(action, MAIN);
    }

    public static <T, R> CompletableFuture<R> thenApplyOnMain(CompletableFuture<T> f, Function<T, R> fn) {
        return f.thenApplyAsync(fn, MAIN);
    }

    public static <T> CompletableFuture<T> whenCompleteOnMain(CompletableFuture<T> f, BiConsumer<? super T, ? super Throwable> action) {
        return f.whenCompleteAsync(action, MAIN);
    }

    public static <T> CompletableFuture<T> exceptionallyOnMain(CompletableFuture<T> f, Function<Throwable, ? extends T> fn) {
        // exceptionallyAsync Java 21'de var; eski sürümlerde thenApplyAsync ile sar.
        return f.handleAsync((v, e) -> e == null ? v : fn.apply(e), MAIN);
    }

    // CancellableFuture için kısayollar (CompletableFuture zaten üst sınıfı)
    public static <T> CompletableFuture<Void> thenAcceptOnMain(CancellableFuture<T> f, Consumer<T> action) {
        return thenAcceptOnMain((CompletableFuture<T>) f, action);
    }

    public static <T, R> CompletableFuture<R> thenApplyOnMain(CancellableFuture<T> f, Function<T, R> fn) {
        return thenApplyOnMain((CompletableFuture<T>) f, fn);
    }

    public static <T> CompletableFuture<T> whenCompleteOnMain(CancellableFuture<T> f, BiConsumer<? super T, ? super Throwable> action) {
        return whenCompleteOnMain((CompletableFuture<T>) f, action);
    }

    public static <T> CompletableFuture<T> exceptionallyOnMain(CancellableFuture<T> f, Function<Throwable, ? extends T> fn) {
        return exceptionallyOnMain((CompletableFuture<T>) f, fn);
    }
}
