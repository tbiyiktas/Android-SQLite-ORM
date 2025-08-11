package lib.persistence;

import androidx.annotation.Nullable;

public abstract class DbResult<T> {

    private DbResult() {}

    public static final class Success<T> extends DbResult<T> {
        private final T data;

        public Success(T data) {
            this.data = data;
        }

        public T getData() {
            return data;
        }
    }

    public static final class Error extends DbResult {
        private final Exception exception;

        public Error(@Nullable Exception exception) {
            this.exception = exception;
        }

        public Exception getException() {
            return exception;
        }
    }
}