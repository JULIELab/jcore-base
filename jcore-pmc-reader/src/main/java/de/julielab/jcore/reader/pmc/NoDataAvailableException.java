package de.julielab.jcore.reader.pmc;

public class NoDataAvailableException extends Exception {

    public NoDataAvailableException() {
    }

    public NoDataAvailableException(String message) {
        super(message);
    }

    public NoDataAvailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoDataAvailableException(Throwable cause) {
        super(cause);
    }

    public NoDataAvailableException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
