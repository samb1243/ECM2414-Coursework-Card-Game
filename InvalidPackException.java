public class InvalidPackException extends Exception {
    private static final long serialVersionUID = 1L;

    public InvalidPackException(String message) {
        super(message);
    }

    public InvalidPackException(String message, Throwable cause) {
        super(message, cause);
    }
}
