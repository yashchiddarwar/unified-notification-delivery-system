package com.Portfolio.Notifire.model.entity;

import com.Portfolio.Notifire.model.enums.NotificationChannel;
import com.Portfolio.Notifire.model.enums.NotificationPriority;
import com.Portfolio.Notifire.model.enums.NotificationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing a notification in the system.
 */
@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_recipient", columnList = "recipient"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_created_at", columnList = "createdAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 255)
    private String recipient;
    
    @Column(length = 500)
    private String subject;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private Template template;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 50, nullable = false)
    private NotificationChannel channel = NotificationChannel.EMAIL;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 50, nullable = false)
    private NotificationStatus status = NotificationStatus.PENDING;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private NotificationPriority priority = NotificationPriority.MEDIUM;
    
    @Column(nullable = false)
    private Integer retryCount = 0;
    
    @Column(nullable = false)
    private Integer maxRetries = 3;
    
    @Column(columnDefinition = "TEXT")
    private String errorMessage;
    
    /**
     * Stores additional metadata as JSON string
     */
    @Column(columnDefinition = "TEXT")
    private String metadata;
    
    private LocalDateTime scheduledAt;
    
    private LocalDateTime sentAt;
    
    private LocalDateTime deliveredAt;
    
    private LocalDateTime failedAt;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    /**
     * Helper method to check if notification can be retried
     */
    public boolean canRetry() {
        return retryCount < maxRetries && 
               (status == NotificationStatus.FAILED || status == NotificationStatus.RETRYING);
    }
    
    /**
     * Helper method to mark notification as sent
     */
    public void markAsSent() {
        this.status = NotificationStatus.SENT;
        this.sentAt = LocalDateTime.now();
    }
    
    /**
     * Helper method to mark notification as failed
     */
    public void markAsFailed(String error) {
        this.status = NotificationStatus.FAILED;
        this.errorMessage = error;
        this.failedAt = LocalDateTime.now();
    }
    
    /**
     * Helper method to increment retry count
     */
    public void incrementRetry() {
        this.retryCount++;
        this.status = NotificationStatus.RETRYING;
    }
}
