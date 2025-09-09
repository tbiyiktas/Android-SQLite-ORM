package lib.persistence;

import org.junit.Test;
import static org.junit.Assert.*;

import lib.persistence.core.DbResult;

public class DbResultTest {

    @Test public void successHoldsData() {
        DbResult<String> r = new DbResult.Success<>("ok");
        assertTrue(r.isSuccess());
        assertFalse(r.isError());
        assertEquals("ok", r.getData());
    }

    @Test public void errorHoldsExceptionAndMessage() {
        Exception ex = new IllegalStateException("boom");
        DbResult<String> r = new DbResult.Error<>(ex);
        assertTrue(r.isError());
        assertFalse(r.isSuccess());
        assertNull(r.getData());
        assertEquals("boom", ((DbResult.Error<?>) r).getErrorMessage());
        assertSame(ex, ((DbResult.Error<?>) r).getException());
    }
}
