package com.vip.finance.retry.exceptions;

public class FundRetryException extends RuntimeException {
    public FundRetryException() {
        super();
    }

    public FundRetryException(String message) {
        super(message);
    }

    public FundRetryException(String message, Throwable cause) {
        super(message, cause);
    }

    public FundRetryException(Throwable cause) {
        super(cause);
    }

    protected FundRetryException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
