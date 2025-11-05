package com.Portfolio.Notifire.service;

import com.Portfolio.Notifire.config.SendGridConfig;
import com.Portfolio.Notifire.model.entity.Notification;
import com.Portfolio.Notifire.model.enums.NotificationStatus;
import com.Portfolio.Notifire.repository.NotificationRepository;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Service for sending emails via SendGrid
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    
    private final SendGrid sendGrid;
    private final SendGridConfig sendGridConfig;
    private final NotificationRepository notificationRepository;
    
    /**
     * Send email asynchronously
     */
    @Async("taskExecutor")
    @Transactional
    public void sendEmailAsync(Long notificationId) {
        log.debug("Processing notification {} asynchronously", notificationId);
        
        Notification notification = notificationRepository.findById(notificationId)
            .orElse(null);
        
        if (notification == null) {
            log.error("Notification {} not found", notificationId);
            return;
        }
        
        sendEmail(notification);
    }
    
    /**
     * Send email synchronously
     */
    @Transactional
    public void sendEmail(Notification notification) {
        if (!sendGridConfig.isEnabled()) {
            log.warn("SendGrid is disabled. Simulating email send for notification {}", notification.getId());
            simulateEmailSend(notification);
            return;
        }
        
        try {
            log.info("Sending email to {} for notification {}", notification.getRecipient(), notification.getId());
            
            // Update status to SENDING
            notification.setStatus(NotificationStatus.SENDING);
            notificationRepository.save(notification);
            
            // Prepare email
            Email from = new Email(sendGridConfig.getFromEmail(), sendGridConfig.getFromName());
            Email to = new Email(notification.getRecipient());
            String subject = notification.getSubject();
            Content content = new Content("text/html", notification.getContent());
            Mail mail = new Mail(from, subject, to, content);
            
            // Send via SendGrid
            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            
            Response response = sendGrid.api(request);
            
            // Check response
            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                notification.markAsSent();
                log.info("Email sent successfully to {} (notification {})", notification.getRecipient(), notification.getId());
            } else {
                String errorMsg = "SendGrid returned status " + response.getStatusCode() + ": " + response.getBody();
                notification.markAsFailed(errorMsg);
                log.error("Failed to send email to {}: {}", notification.getRecipient(), errorMsg);
            }
            
            notificationRepository.save(notification);
            
        } catch (IOException e) {
            String errorMsg = "IOException while sending email: " + e.getMessage();
            notification.markAsFailed(errorMsg);
            notificationRepository.save(notification);
            log.error("Failed to send notification {}: {}", notification.getId(), errorMsg, e);
        } catch (Exception e) {
            String errorMsg = "Unexpected error: " + e.getMessage();
            notification.markAsFailed(errorMsg);
            notificationRepository.save(notification);
            log.error("Unexpected error sending notification {}: {}", notification.getId(), errorMsg, e);
        }
    }
    
    /**
     * Simulate email sending for development/testing
     */
    private void simulateEmailSend(Notification notification) {
        try {
            log.info("SIMULATED: Sending email to {} with subject '{}'", 
                notification.getRecipient(), notification.getSubject());
            log.debug("SIMULATED: Email content: {}", notification.getContent());
            
            // Simulate network delay
            Thread.sleep(500);
            
            // Simulate 90% success rate
            if (Math.random() < 0.9) {
                notification.markAsSent();
                log.info("SIMULATED: Email sent successfully to {}", notification.getRecipient());
            } else {
                notification.markAsFailed("Simulated failure for testing");
                log.warn("SIMULATED: Email failed to {}", notification.getRecipient());
            }
            
            notificationRepository.save(notification);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            notification.markAsFailed("Simulation interrupted");
            notificationRepository.save(notification);
        }
    }
    
    /**
     * Retry failed notification with exponential backoff
     */
    @Async("taskExecutor")
    @Transactional
    public void retryWithBackoff(Long notificationId, int retryAttempt) {
        try {
            // Calculate backoff delay: 2^retryAttempt seconds (1s, 2s, 4s, 8s...)
            long delaySeconds = (long) Math.pow(2, retryAttempt);
            long delayMillis = Math.min(delaySeconds * 1000, 60000); // Max 60 seconds
            
            log.info("Retrying notification {} after {}ms (attempt {})", 
                notificationId, delayMillis, retryAttempt);
            
            Thread.sleep(delayMillis);
            
            Notification notification = notificationRepository.findById(notificationId)
                .orElse(null);
            
            if (notification != null && notification.canRetry()) {
                sendEmail(notification);
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Retry interrupted for notification {}", notificationId);
        }
    }
}
