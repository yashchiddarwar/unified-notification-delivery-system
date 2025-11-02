package com.Portfolio.Notifire.service;

import com.Portfolio.Notifire.dto.NotificationRequest;
import com.Portfolio.Notifire.dto.NotificationResponse;
import com.Portfolio.Notifire.exception.InvalidRequestException;
import com.Portfolio.Notifire.exception.NotificationNotFoundException;
import com.Portfolio.Notifire.exception.TemplateNotFoundException;
import com.Portfolio.Notifire.model.entity.Notification;
import com.Portfolio.Notifire.model.entity.Template;
import com.Portfolio.Notifire.model.enums.NotificationStatus;
import com.Portfolio.Notifire.repository.NotificationRepository;
import com.Portfolio.Notifire.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing notifications
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    
    private final NotificationRepository notificationRepository;
    private final TemplateRepository templateRepository;
    private final TemplateService templateService;
    
    /**
     * Send a new notification
     */
    @Transactional
    public NotificationResponse sendNotification(NotificationRequest request) {
        log.debug("Sending notification to: {}", request.getRecipient());
        
        // Validate request
        validateRequest(request);
        
        // Create notification entity
        Notification notification = new Notification();
        notification.setRecipient(request.getRecipient());
        notification.setPriority(request.getPriority());
        notification.setChannel(request.getChannel());
        notification.setStatus(NotificationStatus.PENDING);
        notification.setScheduledAt(request.getScheduledAt());
        notification.setRetryCount(0);
        notification.setMaxRetries(3);
        
        // Process template if provided
        if (request.getTemplateId() != null) {
            Template template = templateRepository.findById(request.getTemplateId())
                .orElseThrow(() -> new TemplateNotFoundException(request.getTemplateId()));
            
            if (!template.isUsable()) {
                throw new InvalidRequestException("Template is not active: " + template.getName());
            }
            
            notification.setTemplate(template);
            
            // Render template with variables
            String renderedSubject = templateService.renderTemplate(template.getSubject(), request.getVariables());
            String renderedBody = templateService.renderTemplate(template.getBody(), request.getVariables());
            
            notification.setSubject(renderedSubject);
            notification.setContent(renderedBody);
        } else {
            // Use provided content
            notification.setSubject(request.getSubject());
            notification.setContent(request.getContent());
        }
        
        // Save notification
        Notification saved = notificationRepository.save(notification);
        
        log.info("Notification created with id: {} for recipient: {}", saved.getId(), saved.getRecipient());
        
        // TODO: Queue notification for async processing (Day 6)
        
        return mapToResponse(saved, "Notification queued successfully");
    }
    
    /**
     * Get notification by ID
     */
    public NotificationResponse getNotificationById(Long id) {
        log.debug("Fetching notification with id: {}", id);
        
        Notification notification = notificationRepository.findById(id)
            .orElseThrow(() -> new NotificationNotFoundException(id));
        
        return mapToResponse(notification, null);
    }
    
    /**
     * Get all notifications with pagination
     */
    public Page<NotificationResponse> getAllNotifications(Pageable pageable) {
        log.debug("Fetching all notifications with pagination");
        
        return notificationRepository.findAll(pageable)
            .map(notification -> mapToResponse(notification, null));
    }
    
    /**
     * Get notifications by status
     */
    public Page<NotificationResponse> getNotificationsByStatus(NotificationStatus status, Pageable pageable) {
        log.debug("Fetching notifications with status: {}", status);
        
        return notificationRepository.findByStatus(status, pageable)
            .map(notification -> mapToResponse(notification, null));
    }
    
    /**
     * Get notifications by recipient
     */
    public List<NotificationResponse> getNotificationsByRecipient(String recipient) {
        log.debug("Fetching notifications for recipient: {}", recipient);
        
        return notificationRepository.findByRecipient(recipient)
            .stream()
            .map(notification -> mapToResponse(notification, null))
            .collect(Collectors.toList());
    }
    
    /**
     * Retry a failed notification
     */
    @Transactional
    public NotificationResponse retryFailedNotification(Long id) {
        log.debug("Retrying notification with id: {}", id);
        
        Notification notification = notificationRepository.findById(id)
            .orElseThrow(() -> new NotificationNotFoundException(id));
        
        if (!notification.canRetry()) {
            throw new InvalidRequestException("Notification cannot be retried. Max retries reached or status not FAILED.");
        }
        
        notification.incrementRetry();
        notification.setStatus(NotificationStatus.PENDING);
        notification.setErrorMessage(null);
        
        Notification updated = notificationRepository.save(notification);
        
        log.info("Notification {} queued for retry. Attempt: {}", id, updated.getRetryCount());
        
        return mapToResponse(updated, "Notification queued for retry");
    }
    
    /**
     * Get retryable notifications (for background job)
     */
    public List<Notification> getRetryableNotifications() {
        log.debug("Fetching retryable notifications");
        return notificationRepository.findRetryableNotifications();
    }
    
    /**
     * Get today's statistics
     */
    public NotificationStats getTodayStats() {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        
        long sentToday = notificationRepository.countSentToday();
        long failedToday = notificationRepository.countFailedToday();
        double successRate = notificationRepository.getSuccessRate(startOfDay);
        
        return new NotificationStats(sentToday, failedToday, successRate);
    }
    
    /**
     * Validate notification request
     */
    private void validateRequest(NotificationRequest request) {
        if (!request.isValid()) {
            throw new InvalidRequestException("Either content or templateId must be provided");
        }
        
        if (request.getTemplateId() == null && 
            (request.getSubject() == null || request.getSubject().isBlank())) {
            throw new InvalidRequestException("Subject is required when not using a template");
        }
        
        if (request.getScheduledAt() != null && request.getScheduledAt().isBefore(LocalDateTime.now())) {
            throw new InvalidRequestException("Scheduled time cannot be in the past");
        }
    }
    
    /**
     * Map entity to response DTO
     */
    private NotificationResponse mapToResponse(Notification notification, String message) {
        return NotificationResponse.builder()
            .id(notification.getId())
            .recipient(notification.getRecipient())
            .subject(notification.getSubject())
            .status(notification.getStatus())
            .message(message)
            .createdAt(notification.getCreatedAt())
            .sentAt(notification.getSentAt())
            .scheduledAt(notification.getScheduledAt())
            .retryCount(notification.getRetryCount())
            .errorMessage(notification.getErrorMessage())
            .build();
    }
    
    /**
     * Statistics record
     */
    public record NotificationStats(
        long sentToday,
        long failedToday,
        double successRate
    ) {}
}
