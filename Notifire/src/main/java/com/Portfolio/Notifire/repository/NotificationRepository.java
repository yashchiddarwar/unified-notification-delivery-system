package com.Portfolio.Notifire.repository;

import com.Portfolio.Notifire.model.entity.Notification;
import com.Portfolio.Notifire.model.enums.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for Notification entity.
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    /**
     * Find notifications by recipient email
     */
    List<Notification> findByRecipient(String recipient);
    
    /**
     * Find notifications by status
     */
    Page<Notification> findByStatus(NotificationStatus status, Pageable pageable);
    
    /**
     * Find notifications by recipient and status
     */
    List<Notification> findByRecipientAndStatus(String recipient, NotificationStatus status);
    
    /**
     * Find failed notifications that can be retried
     */
    @Query("SELECT n FROM Notification n WHERE n.status = 'FAILED' AND n.retryCount < n.maxRetries")
    List<Notification> findRetryableNotifications();
    
    /**
     * Count notifications by status
     */
    long countByStatus(NotificationStatus status);
    
    /**
     * Find notifications created within a time range
     */
    List<Notification> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    
    /**
     * Find notifications sent today
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.status = 'SENT' AND CAST(n.sentAt AS date) = CURRENT_DATE")
    long countSentToday();
    
    /**
     * Find failed notifications today
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.status = 'FAILED' AND CAST(n.failedAt AS date) = CURRENT_DATE")
    long countFailedToday();
    
    /**
     * Get success rate (percentage)
     */
    @Query("SELECT (COUNT(CASE WHEN n.status = 'SENT' THEN 1 END) * 100.0 / COUNT(*)) " +
           "FROM Notification n WHERE n.createdAt >= :since")
    Double getSuccessRate(@Param("since") LocalDateTime since);
}
