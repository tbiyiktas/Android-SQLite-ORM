package lib.persistence;

import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.Build;

import androidx.test.core.app.ApplicationProvider;

import com.example.adbkit.infrastructure.persistence.DbContext;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import lib.persistence.core.CancellableFuture;
import lib.persistence.core.DbResult;
import lib.persistence.core.IDbContext;

/**
 * İptal senaryolarını iki açıdan sınar:
 *  1) İş başlamadan hemen iptal
 *  2) İş devam ederken iptal
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE) // API 34/36 desteği için Robolectric 4.16+
public class ADbContextRunDbOperationTest {

    static class TestCtx extends DbContext {
        public TestCtx(Context c) { super(c, "test-db-op.db", 5); }
    }

    @Test
    public void cancelWork_beforeStart_marksCancelledOrExceptional() {
        Context ctx = ApplicationProvider.getApplicationContext();
        TestCtx db = new TestCtx(ctx);

        // İş hemen iptal edilecek; işin kendisi biraz geciksin
        CancellableFuture<DbResult<String>> f = db.runDbOperation( (IDbContext.DbWork<String>) (d)  -> {
            Thread.sleep(200);
            return new DbResult.Success<>("LATE");
        }, /*writeTransaction=*/ false);

        // Daha worker başlarken iptal
        f.cancelWork();

        // Küçük bir bekleme: iptal sinyali iş parçacığına ulaşsın
        sleepQuiet(50);

        assertTrue("Future should be cancelled/exceptional (or marked work-cancelled)",
                isCancelledLike(f));

        db.close();
    }

    @Test
    public void cancelWork_duringRun_marksCancelledOrExceptional() {
        Context ctx = ApplicationProvider.getApplicationContext();
        TestCtx db = new TestCtx(ctx);

        CancellableFuture<DbResult<String>> f = db.runDbOperation( (IDbContext.DbWork<String>) (d)  -> {
            // İş biraz çalışsın sonra iptal gelecek
            Thread.sleep(50);
            Thread.sleep(200);
            return new DbResult.Success<>("LATE");
        }, /*writeTransaction=*/ false);

        // İşin başlaması için kısa bekleme, sonra iptal
        sleepQuiet(20);
        f.cancelWork();

        // İptal sonuçlarının yansıması için kısa bekleme
        sleepQuiet(50);

        assertTrue("Future should be cancelled/exceptional (or marked work-cancelled)",
                isCancelledLike(f));

        db.close();
    }

    // --- helpers ---

    private static void sleepQuiet(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }

    /** CancellableFuture'ın "iptal sayılır" hallerini tek yerden kontrol et. */
    private static boolean isCancelledLike(CancellableFuture<?> f) {
        boolean cancelled = f.isCancelled() || f.isCompletedExceptionally();
        try {
            // Eğer CancellableFuture public bir isWorkCancelled() sağlıyorsa, onu da dikkate al.
            java.lang.reflect.Method m = f.getClass().getMethod("isWorkCancelled");
            Object v = m.invoke(f);
            if (v instanceof Boolean) {
                cancelled = cancelled || (Boolean) v;
            }
        } catch (Throwable ignore) {
            // method yoksa problem değil
        }
        return cancelled;
    }
}

/*
package lib.persistence;

import static org.junit.Assert.*;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import com.example.adbkit.MyApplication;

import lib.persistence.core.CancellableFuture;
import lib.persistence.core.DbResult;

@RunWith(RobolectricTestRunner.class)
@Config(application = MyApplication.class)
public class ADbContextRunDbOperationTest {

    static class TestCtx extends com.example.adbkit.DbContext {
        public TestCtx(Context c) { super(c, "test-db-op.db", 5); }
    }

    @Test public void run_readOperation_returnsSuccess() {
        Context ctx = ApplicationProvider.getApplicationContext();
        TestCtx db = new TestCtx(ctx);
        CancellableFuture<DbResult<String>> f = db.runDbOperation(d -> new DbResult.Success<>("OK"), false);
        DbResult<String> r = f.join();
        assertTrue(r.isSuccess());
        db.close();
    }

    @Test public void cancelWork_marksFutureCancelledOrExceptional() {
        Context ctx = ApplicationProvider.getApplicationContext();
        TestCtx db = new TestCtx(ctx);
        CancellableFuture<DbResult<String>> f = db.runDbOperation(d -> {
            Thread.sleep(200);
            return new DbResult.Success<>("LATE");
        }, false);
        f.cancelWork();
        assertTrue(f.isCancelled() || f.isCompletedExceptionally());
        db.close();
    }
}
*/