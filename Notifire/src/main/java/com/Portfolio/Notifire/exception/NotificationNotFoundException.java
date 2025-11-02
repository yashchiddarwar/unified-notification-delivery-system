package com.Portfolio.Notifire.exception;

/**
 * Exception thrown when a notification is not found
 */
public class NotificationNotFoundException extends RuntimeException {
    
    public NotificationNotFoundException(Long id) {
        super("Notification not found with id: " + id);
    }
    
    public NotificationNotFoundException(String message) {
        super(message);
    }
}
