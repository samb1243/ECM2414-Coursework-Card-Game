package src;
public class InvalidPackException extends Exception { //class for the exception thrown when a pack of cards is invalid, extends the Exception class
    private static final long serialVersionUID = 1L;

    public InvalidPackException(String message) {
        super(message);
    }

    public InvalidPackException(String message, Throwable cause) {
        super(message, cause);
    }
}
