package testutil;

import android.os.Looper;
import org.robolectric.Shadows;

public final class UiTestHelpers {
    private UiTestHelpers() {}
    public static void flushUi() {
        Shadows.shadowOf(Looper.getMainLooper()).idle();
    }
}
