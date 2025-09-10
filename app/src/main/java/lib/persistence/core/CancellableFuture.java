// lib/persistence/core/CancellableFuture.java
package lib.persistence.core;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * İptal-bilinçli CompletableFuture.
 * - cancelWork(): iş-seviyesi iptal işaretler ve Future'ı da iptal eder.
 * - isWorkCancelled(): iş-iptal bayrağı veya Future iptali true ise true döner.
 * - complete/completeExceptionally: iptal edilmişse tamamlamaz.
 */
public class CancellableFuture<T> extends CompletableFuture<T>
        implements WorkCancellationAware {

    private final AtomicBoolean workCancelled = new AtomicBoolean(false);

    /** İş iptal isteği (idempotent). Future'ı da iptal eder. */
    public boolean cancelWork() {
        if (workCancelled.compareAndSet(false, true)) {
            super.cancel(true); // mayInterruptIfRunning=true
            return true;
        }
        return false;
    }

    /** Future.cancel(...) doğrudan çağrılırsa da işaretleyelim. */
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        workCancelled.set(true);
        return super.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isWorkCancelled() {
        // İş-iptal bayrağı veya Future iptali
        return workCancelled.get() || isCancelled();
    }

    @Override
    public boolean complete(T value) {
        return !isWorkCancelled() && super.complete(value);
    }

    @Override
    public boolean completeExceptionally(Throwable ex) {
        return !isWorkCancelled() && super.completeExceptionally(ex);
    }
}
