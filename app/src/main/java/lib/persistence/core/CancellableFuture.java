package lib.persistence.core;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class CancellableFuture<T> extends CompletableFuture<T> {
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    /**
     * İş iptal istekçisi (idempotent).
     */
    public boolean cancelWork() {
        if (cancelled.compareAndSet(false, true)) {
            super.cancel(true); // CompletableFuture işaretlemesi
            return true;
        }
        return false;
    }

    public boolean isWorkCancelled() {
        return cancelled.get();
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
