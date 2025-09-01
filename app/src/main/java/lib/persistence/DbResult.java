package lib.persistence;

import androidx.annotation.Nullable;

public abstract class DbResult<T> {

    private DbResult() {}
    public abstract boolean isSuccess();
    public abstract boolean isError();
    public abstract T getData();

    public static final class Success<T> extends DbResult<T> {
        private final T data;

        public Success(T data) {
            this.data = data;
        }
        @Override
        public boolean isSuccess() {
            return true;
        }

        @Override
        public boolean isError() {
            return false;
        }

        @Override
        public T getData() {
            return data;
        }
    }

    public static final class Error<T> extends DbResult<T> {
        private final Exception exception;
        private final String errorMessage;

        public Error(@Nullable Exception exception, String errorMessage) {
            this.exception = exception;
            this.errorMessage = errorMessage;
        }

        public Error(@Nullable Exception exception) {
            this.exception = exception;
            this.errorMessage = exception.getMessage();
        }

        public Exception getException() {
            return exception;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        @Override
        public boolean isSuccess() {
            return false;
        }

        @Override
        public boolean isError() {
            return true;
        }

        @Override
        public T getData() {
            return null;
        }
    }
}