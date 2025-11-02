package com.Portfolio.Notifire.model.enums;

/**
 * Represents the current status of a notification.
 */
public enum NotificationStatus {
    /**
     * Notification has been created and is waiting to be sent
     */
    PENDING,
    
    /**
     * Notification is currently being sent
     */
    SENDING,
    
    /**
     * Notification has been successfully sent
     */
    SENT,
    
    /**
     * Notification failed to send
     */
    FAILED,
    
    /**
     * Notification is being retried after a failure
     */
    RETRYING
}
