package de.julielab.jcore.reader;

public class MissingInfonException extends Exception {
    public MissingInfonException() {
    }

    public MissingInfonException(String message) {
        super(message);
    }

    public MissingInfonException(String message, Throwable cause) {
        super(message, cause);
    }

    public MissingInfonException(Throwable cause) {
        super(cause);
    }

    public MissingInfonException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
