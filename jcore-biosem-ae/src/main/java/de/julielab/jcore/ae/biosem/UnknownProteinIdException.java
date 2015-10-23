package de.julielab.jcore.ae.biosem;

/**
 * Sometimes, BioSem returns protein IDs which it has not been delivered. This
 * exception serves to identify this particular error.
 * 
 * @author faessler
 * 
 */
public class UnknownProteinIdException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4887878549857216157L;

	public UnknownProteinIdException() {
		super();
	}

	public UnknownProteinIdException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public UnknownProteinIdException(String message, Throwable cause) {
		super(message, cause);
	}

	public UnknownProteinIdException(String message) {
		super(message);
	}

	public UnknownProteinIdException(Throwable cause) {
		super(cause);
	}

}
