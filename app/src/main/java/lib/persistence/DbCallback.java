package lib.persistence;


public interface DbCallback<T> {
    void onResult(DbResult<T> result);
}