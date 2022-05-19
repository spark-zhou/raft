package com.spark.raft.core.log;

public class LogException extends RuntimeException {

    public LogException() {
    }

    /**
     * Create.
     *
     * @param message message
     */
    public LogException(String message) {
        super(message);
    }

    /**
     * Create.
     *
     * @param cause cause
     */
    public LogException(Throwable cause) {
        super(cause);
    }

    /**
     * Create.
     *
     * @param message message
     * @param cause cause
     */
    public LogException(String message, Throwable cause) {
        super(message, cause);
    }
}
