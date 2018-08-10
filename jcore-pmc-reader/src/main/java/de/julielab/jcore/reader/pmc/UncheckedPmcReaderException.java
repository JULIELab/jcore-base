package de.julielab.jcore.reader.pmc;

public class UncheckedPmcReaderException extends RuntimeException {
    public UncheckedPmcReaderException() {
    }

    public UncheckedPmcReaderException(String message) {
        super(message);
    }

    public UncheckedPmcReaderException(String message, Throwable cause) {
        super(message, cause);
    }

    public UncheckedPmcReaderException(Throwable cause) {
        super(cause);
    }

    public UncheckedPmcReaderException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
