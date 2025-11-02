package com.Portfolio.Notifire.model.enums;

/**
 * Represents the priority level of a notification.
 */
public enum NotificationPriority {
    /**
     * Low priority - can be delayed
     */
    LOW,
    
    /**
     * Medium priority - normal processing
     */
    MEDIUM,
    
    /**
     * High priority - should be processed immediately
     */
    HIGH
}
