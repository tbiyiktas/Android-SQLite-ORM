package lib.concurrent;


import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * İptal-bilinçli CompletableFuture:
 * - İdempotent iş iptali: cancelWork()  -> işaretler + bağlı kaynakları iptal eder + CF'ı iptal eder
 * - CF iptali override: cancel(...)     -> aynı davranış (kancalar/Cancelables çalışır)
 * - complete/completeExceptionally      -> iş iptal edilmişse yutulur (tamamlamaz)
 * - thenApplyC/thenComposeC             -> tip korur, iptal yayılımı ve kanca devri yapar
 * - wrap/supplyAsyncC/completed/failed  -> yardımcılar
 */
public class CancellableFuture<T> extends CompletableFuture<T>
        implements WorkCancellationAware {

    /** İş-iptal (work-level) bayrağı. */
    private final AtomicBoolean workCancelled = new AtomicBoolean(false);

    /** İptalde çağrılacak opsiyonel kanca. */
    private final AtomicReference<Runnable> cancelHook = new AtomicReference<>(null);

    /** İptal edilmesi gereken dış kaynaklar (komut, socket, vs.). */
    private final List<Cancelable> cancelables = new CopyOnWriteArrayList<>();

    /* ======================= Public API ======================= */

    /** İş iptal isteği (idempotent). Bağlı kaynakları ve future'ı da iptal eder. */
    @Override
    public boolean cancelWork() {
        return cancelInternal(true);
    }

    /** Future.cancel(...) çağrılırsa da aynı davranış: kancaları/ kaynakları çalıştır. */
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return cancelInternal(mayInterruptIfRunning);
    }

    /** İş-iptal bayrağı veya CF iptali true ise true. */
    @Override
    public boolean isWorkCancelled() {
        return workCancelled.get() || isCancelled();
    }

    /** İptal kancası ekle (son eklenen geçerli olur). */
    public void addCancelHook(Runnable hook) {
        if (hook != null) this.cancelHook.set(hook);
    }

    /** Dış iptal edilebilir kaynağı iliştir (birden çok eklenebilir). */
    public void attachCancelable(Cancelable c) {
        if (c != null) this.cancelables.add(c);
    }

    /** Başka bir CancellableFuture'dan iptal kancası ve cancelable'ları devral. */
    public void inheritCancellationFrom(CancellableFuture<?> other) {
        if (other == null) return;
        Runnable r = other.cancelHook.get();
        if (r != null) this.cancelHook.set(r);
        this.cancelables.addAll(other.cancelables);
    }

    /** Tip koruyan map. */
    public <U> CancellableFuture<U> thenApplyC(Function<? super T, ? extends U> fn) {
        CancellableFuture<U> next = new CancellableFuture<>();
        next.inheritCancellationFrom(this);

        this.whenComplete((val, ex) -> {
            if (this.isWorkCancelled()) {
                next.cancel(true);
                return;
            }
            if (ex != null) {
                next.completeExceptionally(ex);
                return;
            }
            try {
                U mapped = fn.apply(val);
                next.complete(mapped);
            } catch (Throwable t) {
                next.completeExceptionally(t);
            }
        });

        // Kullanıcı next'i iptal ederse, paylaşılan kaynaklar üzerinden upstream de iptal olur.
        next.whenComplete((v, e) -> {
            if (next.isCancelled() && !this.isCancelled()) {
                // Paylaşılan cancelable'lar zaten aynı; yine de CF iptali tetikleyelim.
                try { this.cancel(true); } catch (Throwable ignored) {}
            }
        });
        return next;
    }

    /** Tip koruyan compose. */
    public <U> CancellableFuture<U> thenComposeC(Function<? super T, ? extends CancellableFuture<U>> fn) {
        CancellableFuture<U> next = new CancellableFuture<>();
        next.inheritCancellationFrom(this);

        this.whenComplete((val, ex) -> {
            if (this.isWorkCancelled()) {
                next.cancel(true);
                return;
            }
            if (ex != null) {
                next.completeExceptionally(ex);
                return;
            }
            try {
                CancellableFuture<U> inner = fn.apply(val);
                if (inner == null) {
                    next.complete(null);
                    return;
                }

                // İptal kancaları/cancelable'lar zincire taşınsın
                next.inheritCancellationFrom(inner);

                // inner tamamlandığında next'e aktar
                inner.whenComplete((u, ex2) -> {
                    if (inner.isWorkCancelled()) next.cancel(true);
                    else if (ex2 != null) next.completeExceptionally(ex2);
                    else next.complete(u);
                });

                // next iptal edilirse inner'ı da iptal et
                next.whenComplete((u, e2) -> {
                    if (next.isCancelled() && !inner.isCancelled()) {
                        try { inner.cancel(true); } catch (Throwable ignored) {}
                    }
                });
            } catch (Throwable t) {
                next.completeExceptionally(t);
            }
        });
        return next;
    }

    /* ======================= Helpers ======================= */

    /** Hızlı başarı. */
    public static <T> CancellableFuture<T> completed(T value) {
        CancellableFuture<T> f = new CancellableFuture<>();
        f.complete(value);
        return f;
    }

    /** Hızlı hata. */
    public static <T> CancellableFuture<T> failed(Throwable error) {
        CancellableFuture<T> f = new CancellableFuture<>();
        f.completeExceptionally(error);
        return f;
    }

    /**
     * Var olan CompletableFuture'ı sar. Tamamlama ve iptal **çift yönlü** bağlanır.
     */
    public static <T> CancellableFuture<T> wrap(CompletableFuture<T> src) {
        CancellableFuture<T> dst = new CancellableFuture<>();
        src.whenComplete((v, e) -> {
            if (e != null) dst.completeExceptionally(e);
            else dst.complete(v);
        });
        // dst iptal edilirse src iptal edilsin
        dst.whenComplete((v, e) -> {
            if (dst.isCancelled() && !src.isCancelled()) {
                try { src.cancel(true); } catch (Throwable ignored) {}
            }
        });
        // src iptal edilirse dst iptal edilsin
        src.whenComplete((v, e) -> {
            if (src.isCancelled() && !dst.isCancelled()) {
                try { dst.cancel(true); } catch (Throwable ignored) {}
            }
        });
        return dst;
    }

    /** Default executor ile async üretim (iptal kontrolü içerir). */
    public static <T> CancellableFuture<T> supplyAsyncC(Supplier<T> supplier) {
        return supplyAsyncC(supplier, ForkJoinPool.commonPool());
    }

    /** Özel executor ile async üretim (iptal kontrolü içerir). */
    public static <T> CancellableFuture<T> supplyAsyncC(Supplier<T> supplier, Executor executor) {
        CancellableFuture<T> f = new CancellableFuture<>();
        CompletableFuture.supplyAsync(() -> {
            if (f.isWorkCancelled()) throw new CancellationException();
            return supplier.get();
        }, executor).whenComplete((v, e) -> {
            if (e != null) f.completeExceptionally(e);
            else f.complete(v);
        });
        return f;
    }

    /* ======================= Overrides ======================= */

    /** İptal edildiyse tamamlamayı yut. */
    @Override
    public boolean complete(T value) {
        return !isWorkCancelled() && super.complete(value);
    }

    /** İptal edildiyse hata ile tamamlamayı yut. */
    @Override
    public boolean completeExceptionally(Throwable ex) {
        return !isWorkCancelled() && super.completeExceptionally(ex);
    }

    /* ======================= Internal ======================= */

    private boolean cancelInternal(boolean mayInterruptIfRunning) {
        boolean first = workCancelled.compareAndSet(false, true);

        // Kancayı bir kere çalıştır
        Runnable hook = cancelHook.getAndSet(null);
        if (hook != null) {
            try { hook.run(); } catch (Throwable ignored) {}
        }

        // Dış kaynakları iptal et
        for (Cancelable c : cancelables) {
            try { c.cancel(); } catch (Throwable ignored) {}
        }

        // CompletableFuture iptal et
        boolean cf = super.cancel(mayInterruptIfRunning);

        // "İlk kez iptal" veya CF dönüşü true ise true dön
        return first || cf;
    }
}
