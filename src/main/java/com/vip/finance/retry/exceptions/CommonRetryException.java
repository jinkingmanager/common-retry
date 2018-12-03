package com.vip.finance.retry.exceptions;

public class CommonRetryException extends RuntimeException {
    public CommonRetryException() {
        super();
    }

    public CommonRetryException(String message) {
        super(message);
    }

    public CommonRetryException(String message, Throwable cause) {
        super(message, cause);
    }

    public CommonRetryException(Throwable cause) {
        super(cause);
    }

    protected CommonRetryException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
