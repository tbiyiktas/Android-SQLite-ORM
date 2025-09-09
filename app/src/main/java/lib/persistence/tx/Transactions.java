// lib/persistence/tx/Transactions.java
package lib.persistence.tx;

import android.database.sqlite.SQLiteDatabase;
import java.util.function.Function;
import lib.persistence.core.CancellableFuture;
import lib.persistence.core.DbResult;
import lib.persistence.core.IDbContext;
import lib.persistence.core.IDbContext.DbWork;

public final class Transactions {
    private Transactions() {}
//    public static <R> CancellableFuture<DbResult<R>> run(IDbContext ctx, Function<SQLiteDatabase, R> work) {
//        return ctx.runDbOperation((DbWork<R>) db -> {
//            R out = work.apply(db);
//            return new DbResult.Success<>(out);
//        }, true);
//    }

    public static <R> CancellableFuture<DbResult<R>> run(IDbContext ctx, DbWork<R> work) {
        return ctx.runDbOperation(work, /*writeTransaction=*/ true);
    }
}
