package com.Portfolio.Notifire.service;

import com.Portfolio.Notifire.config.SendGridConfig;
import com.Portfolio.Notifire.model.entity.Notification;
import com.Portfolio.Notifire.model.enums.NotificationChannel;
import com.Portfolio.Notifire.model.enums.NotificationPriority;
import com.Portfolio.Notifire.model.enums.NotificationStatus;
import com.Portfolio.Notifire.repository.NotificationRepository;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EmailService
 */
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {
    
    @Mock
    private SendGrid sendGrid;
    
    @Mock
    private SendGridConfig sendGridConfig;
    
    @Mock
    private NotificationRepository notificationRepository;
    
    @InjectMocks
    private EmailService emailService;
    
    private Notification testNotification;
    
    @BeforeEach
    void setUp() {
        testNotification = new Notification();
        testNotification.setId(1L);
        testNotification.setRecipient("test@example.com");
        testNotification.setSubject("Test Subject");
        testNotification.setContent("Test Content");
        testNotification.setStatus(NotificationStatus.PENDING);
        testNotification.setChannel(NotificationChannel.EMAIL);
        testNotification.setPriority(NotificationPriority.MEDIUM);
        testNotification.setRetryCount(0);
        testNotification.setMaxRetries(3);
    }
    
    @Test
    void testSendEmail_SendGridDisabled_Simulates() throws IOException {
        // Given
        when(sendGridConfig.isEnabled()).thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
        
        // When
        emailService.sendEmail(testNotification);
        
        // Then
        verify(notificationRepository, atLeastOnce()).save(any(Notification.class));
        verify(sendGrid, never()).api(any());
    }
    
    @Test
    void testSendEmail_SendGridEnabled_Success() throws IOException {
        // Given
        when(sendGridConfig.isEnabled()).thenReturn(true);
        when(sendGridConfig.getFromEmail()).thenReturn("noreply@test.com");
        when(sendGridConfig.getFromName()).thenReturn("Test Service");
        
        Response mockResponse = new Response();
        mockResponse.setStatusCode(202);
        when(sendGrid.api(any())).thenReturn(mockResponse);
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
        
        // When
        emailService.sendEmail(testNotification);
        
        // Then
        verify(sendGrid, times(1)).api(any());
        verify(notificationRepository, times(2)).save(any(Notification.class));
        assertThat(testNotification.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(testNotification.getSentAt()).isNotNull();
    }
    
    @Test
    void testSendEmail_SendGridEnabled_Failure() throws IOException {
        // Given
        when(sendGridConfig.isEnabled()).thenReturn(true);
        when(sendGridConfig.getFromEmail()).thenReturn("noreply@test.com");
        when(sendGridConfig.getFromName()).thenReturn("Test Service");
        
        Response mockResponse = new Response();
        mockResponse.setStatusCode(400);
        mockResponse.setBody("Bad Request");
        when(sendGrid.api(any())).thenReturn(mockResponse);
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
        
        // When
        emailService.sendEmail(testNotification);
        
        // Then
        verify(sendGrid, times(1)).api(any());
        verify(notificationRepository, times(2)).save(any(Notification.class));
        assertThat(testNotification.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(testNotification.getErrorMessage()).contains("400");
    }
    
    @Test
    void testSendEmail_IOException() throws IOException {
        // Given
        when(sendGridConfig.isEnabled()).thenReturn(true);
        when(sendGridConfig.getFromEmail()).thenReturn("noreply@test.com");
        when(sendGridConfig.getFromName()).thenReturn("Test Service");
        when(sendGrid.api(any())).thenThrow(new IOException("Network error"));
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
        
        // When
        emailService.sendEmail(testNotification);
        
        // Then
        verify(notificationRepository, times(2)).save(any(Notification.class));
        assertThat(testNotification.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(testNotification.getErrorMessage()).contains("IOException");
    }
    
    @Test
    void testSendEmailAsync_NotificationNotFound() throws IOException {
        // Given
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());
        
        // When
        emailService.sendEmailAsync(999L);
        
        // Then
        verify(notificationRepository, times(1)).findById(999L);
        verify(sendGrid, never()).api(any());
    }
    
    @Test
    void testSendEmailAsync_NotificationFound() {
        // Given
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));
        when(sendGridConfig.isEnabled()).thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
        
        // When
        emailService.sendEmailAsync(1L);
        
        // Then - Give async method time to execute
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        verify(notificationRepository, times(1)).findById(1L);
    }
    
    @Test
    void testRetryWithBackoff_CanRetry() {
        // Given
        testNotification.setStatus(NotificationStatus.FAILED);
        testNotification.setRetryCount(1);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));
        when(sendGridConfig.isEnabled()).thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
        
        // When
        emailService.retryWithBackoff(1L, 1);
        
        // Then - Give async method time to execute
        try {
            Thread.sleep(3000); // Wait for backoff delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        verify(notificationRepository, atLeastOnce()).findById(1L);
    }
}
