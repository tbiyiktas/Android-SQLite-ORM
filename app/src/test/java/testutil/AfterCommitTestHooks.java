package testutil;

public final class AfterCommitTestHooks {
    public static void reset() {
        lib.persistence.tx.AfterCommitEvents.clear();
    }
}