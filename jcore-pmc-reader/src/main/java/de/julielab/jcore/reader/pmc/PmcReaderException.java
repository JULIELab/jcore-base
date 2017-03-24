package de.julielab.jcore.reader.pmc;

public class PmcReaderException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public PmcReaderException() {
		super();
	}

	public PmcReaderException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public PmcReaderException(String message, Throwable cause) {
		super(message, cause);
	}

	public PmcReaderException(String message) {
		super(message);
	}

	public PmcReaderException(Throwable cause) {
		super(cause);
	}

}
