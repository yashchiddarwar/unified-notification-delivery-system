package com.Portfolio.Notifire.service;

import com.Portfolio.Notifire.model.entity.Notification;
import com.Portfolio.Notifire.model.enums.NotificationStatus;
import com.Portfolio.Notifire.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled job service for processing notifications
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationScheduler {
    
    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    
    /**
     * Process pending notifications every 30 seconds
     */
    @Scheduled(fixedDelay = 30000, initialDelay = 10000)
    public void processPendingNotifications() {
        log.debug("Processing pending notifications...");
        
        List<Notification> pending = notificationRepository.findByStatus(
            NotificationStatus.PENDING, 
            org.springframework.data.domain.PageRequest.of(0, 10)
        ).getContent();
        
        if (pending.isEmpty()) {
            log.debug("No pending notifications to process");
            return;
        }
        
        log.info("Found {} pending notifications to process", pending.size());
        
        for (Notification notification : pending) {
            // Check if scheduled for future
            if (notification.getScheduledAt() != null && 
                notification.getScheduledAt().isAfter(LocalDateTime.now())) {
                log.debug("Notification {} is scheduled for {}, skipping", 
                    notification.getId(), notification.getScheduledAt());
                continue;
            }
            
            log.info("Processing pending notification {}", notification.getId());
            emailService.sendEmailAsync(notification.getId());
        }
    }
    
    /**
     * Retry failed notifications every 2 minutes
     */
    @Scheduled(fixedDelay = 120000, initialDelay = 60000)
    public void retryFailedNotifications() {
        log.debug("Checking for retryable notifications...");
        
        List<Notification> retryable = notificationRepository.findRetryableNotifications();
        
        if (retryable.isEmpty()) {
            log.debug("No failed notifications to retry");
            return;
        }
        
        log.info("Found {} notifications eligible for retry", retryable.size());
        
        for (Notification notification : retryable) {
            if (notification.canRetry()) {
                log.info("Retrying failed notification {} (attempt {}/{})", 
                    notification.getId(), 
                    notification.getRetryCount() + 1, 
                    notification.getMaxRetries());
                
                notification.incrementRetry();
                notification.setStatus(NotificationStatus.RETRYING);
                notificationRepository.save(notification);
                
                // Retry with exponential backoff
                emailService.retryWithBackoff(notification.getId(), notification.getRetryCount());
            }
        }
    }
    
    /**
     * Log statistics every 5 minutes
     */
    @Scheduled(fixedDelay = 300000, initialDelay = 30000)
    public void logStatistics() {
        long pending = notificationRepository.countByStatus(NotificationStatus.PENDING);
        long sending = notificationRepository.countByStatus(NotificationStatus.SENDING);
        long sent = notificationRepository.countByStatus(NotificationStatus.SENT);
        long failed = notificationRepository.countByStatus(NotificationStatus.FAILED);
        long retrying = notificationRepository.countByStatus(NotificationStatus.RETRYING);
        
        long sentToday = notificationRepository.countSentToday();
        long failedToday = notificationRepository.countFailedToday();
        
        log.info("=== Notification Statistics ===");
        log.info("Status - PENDING: {}, SENDING: {}, SENT: {}, FAILED: {}, RETRYING: {}", 
            pending, sending, sent, failed, retrying);
        log.info("Today - Sent: {}, Failed: {}", sentToday, failedToday);
        log.info("===============================");
    }
}
