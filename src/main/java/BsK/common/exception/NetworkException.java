package BsK.common.exception;

/**
 * Unchecked exception thrown when a network operation fails.
 * Contains a user-friendly message suitable for display in dialogs or toasts.
 */
public class NetworkException extends RuntimeException {
    
    public NetworkException(String message) {
        super(message);
    }
    
    public NetworkException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Get a user-friendly message suitable for display
     */
    public String getUserMessage() {
        return getMessage();
    }
}

