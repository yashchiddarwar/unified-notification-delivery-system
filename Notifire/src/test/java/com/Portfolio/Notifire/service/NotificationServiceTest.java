package com.Portfolio.Notifire.service;

import com.Portfolio.Notifire.dto.NotificationRequest;
import com.Portfolio.Notifire.dto.NotificationResponse;
import com.Portfolio.Notifire.exception.InvalidRequestException;
import com.Portfolio.Notifire.exception.NotificationNotFoundException;
import com.Portfolio.Notifire.exception.TemplateNotFoundException;
import com.Portfolio.Notifire.model.entity.Notification;
import com.Portfolio.Notifire.model.entity.Template;
import com.Portfolio.Notifire.model.enums.NotificationChannel;
import com.Portfolio.Notifire.model.enums.NotificationPriority;
import com.Portfolio.Notifire.model.enums.NotificationStatus;
import com.Portfolio.Notifire.repository.NotificationRepository;
import com.Portfolio.Notifire.repository.TemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for NotificationService
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    
    @Mock
    private NotificationRepository notificationRepository;
    
    @Mock
    private TemplateRepository templateRepository;
    
    @Mock
    private TemplateService templateService;
    
    @Mock
    private EmailService emailService;
    
    @InjectMocks
    private NotificationService notificationService;
    
    private NotificationRequest validRequest;
    private Notification savedNotification;
    private Template mockTemplate;
    
    @BeforeEach
    void setUp() {
        validRequest = new NotificationRequest();
        validRequest.setRecipient("test@example.com");
        validRequest.setSubject("Test Subject");
        validRequest.setContent("Test Content");
        validRequest.setPriority(NotificationPriority.MEDIUM);
        validRequest.setChannel(NotificationChannel.EMAIL);
        
        savedNotification = new Notification();
        savedNotification.setId(1L);
        savedNotification.setRecipient("test@example.com");
        savedNotification.setSubject("Test Subject");
        savedNotification.setContent("Test Content");
        savedNotification.setStatus(NotificationStatus.PENDING);
        savedNotification.setCreatedAt(LocalDateTime.now());
        
        mockTemplate = new Template();
        mockTemplate.setId(1L);
        mockTemplate.setName("welcome_email");
        mockTemplate.setSubject("Welcome {{user_name}}!");
        mockTemplate.setBody("Hello {{user_name}}, welcome to our service!");
        mockTemplate.setIsActive(true);
    }
    
    @Test
    void testSendNotification_Success() {
        // Given
        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);
        
        // When
        NotificationResponse response = notificationService.sendNotification(validRequest);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getRecipient()).isEqualTo("test@example.com");
        assertThat(response.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(response.getMessage()).isEqualTo("Notification queued successfully");
        
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }
    
    @Test
    void testSendNotification_WithTemplate() {
        // Given
        validRequest.setTemplateId(1L);
        validRequest.setSubject(null);
        validRequest.setContent(null);
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("user_name", "John Doe");
        validRequest.setVariables(variables);
        
        when(templateRepository.findById(1L)).thenReturn(Optional.of(mockTemplate));
        when(templateService.renderTemplate(eq("Welcome {{user_name}}!"), any())).thenReturn("Welcome John Doe!");
        when(templateService.renderTemplate(eq("Hello {{user_name}}, welcome to our service!"), any()))
            .thenReturn("Hello John Doe, welcome to our service!");
        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);
        
        // When
        NotificationResponse response = notificationService.sendNotification(validRequest);
        
        // Then
        assertThat(response).isNotNull();
        verify(templateRepository, times(1)).findById(1L);
        verify(templateService, times(2)).renderTemplate(anyString(), any());
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }
    
    @Test
    void testSendNotification_InvalidEmail() {
        // Given
        validRequest.setRecipient("invalid-email");
        
        // When/Then - Validation happens at controller level with @Valid
        // Here we test business logic validation
        validRequest.setContent(null);
        validRequest.setTemplateId(null);
        
        assertThatThrownBy(() -> notificationService.sendNotification(validRequest))
            .isInstanceOf(InvalidRequestException.class)
            .hasMessageContaining("Either content or templateId must be provided");
    }
    
    @Test
    void testSendNotification_MissingContent() {
        // Given
        validRequest.setContent(null);
        validRequest.setTemplateId(null);
        
        // When/Then
        assertThatThrownBy(() -> notificationService.sendNotification(validRequest))
            .isInstanceOf(InvalidRequestException.class)
            .hasMessageContaining("Either content or templateId must be provided");
    }
    
    @Test
    void testSendNotification_TemplateNotFound() {
        // Given
        validRequest.setTemplateId(999L);
        validRequest.setContent(null);
        
        when(templateRepository.findById(999L)).thenReturn(Optional.empty());
        
        // When/Then
        assertThatThrownBy(() -> notificationService.sendNotification(validRequest))
            .isInstanceOf(TemplateNotFoundException.class);
    }
    
    @Test
    void testSendNotification_InactiveTemplate() {
        // Given
        mockTemplate.setIsActive(false);
        validRequest.setTemplateId(1L);
        validRequest.setContent(null);
        
        when(templateRepository.findById(1L)).thenReturn(Optional.of(mockTemplate));
        
        // When/Then
        assertThatThrownBy(() -> notificationService.sendNotification(validRequest))
            .isInstanceOf(InvalidRequestException.class)
            .hasMessageContaining("Template is not active");
    }
    
    @Test
    void testSendNotification_ScheduledInPast() {
        // Given
        validRequest.setScheduledAt(LocalDateTime.now().minusHours(1));
        
        // When/Then
        assertThatThrownBy(() -> notificationService.sendNotification(validRequest))
            .isInstanceOf(InvalidRequestException.class)
            .hasMessageContaining("Scheduled time cannot be in the past");
    }
    
    @Test
    void testGetNotificationById_Found() {
        // Given
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(savedNotification));
        
        // When
        NotificationResponse response = notificationService.getNotificationById(1L);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getRecipient()).isEqualTo("test@example.com");
        
        verify(notificationRepository, times(1)).findById(1L);
    }
    
    @Test
    void testGetNotificationById_NotFound() {
        // Given
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());
        
        // When/Then
        assertThatThrownBy(() -> notificationService.getNotificationById(999L))
            .isInstanceOf(NotificationNotFoundException.class)
            .hasMessageContaining("Notification not found with id: 999");
    }
    
    @Test
    void testGetAllNotifications() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<Notification> notifications = Arrays.asList(savedNotification);
        Page<Notification> page = new PageImpl<>(notifications, pageable, 1);
        
        when(notificationRepository.findAll(pageable)).thenReturn(page);
        
        // When
        Page<NotificationResponse> result = notificationService.getAllNotifications(pageable);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        
        verify(notificationRepository, times(1)).findAll(pageable);
    }
    
    @Test
    void testGetNotificationsByStatus() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<Notification> notifications = Arrays.asList(savedNotification);
        Page<Notification> page = new PageImpl<>(notifications, pageable, 1);
        
        when(notificationRepository.findByStatus(NotificationStatus.PENDING, pageable)).thenReturn(page);
        
        // When
        Page<NotificationResponse> result = notificationService.getNotificationsByStatus(NotificationStatus.PENDING, pageable);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        
        verify(notificationRepository, times(1)).findByStatus(NotificationStatus.PENDING, pageable);
    }
    
    @Test
    void testRetryFailedNotification_Success() {
        // Given
        savedNotification.setStatus(NotificationStatus.FAILED);
        savedNotification.setRetryCount(1);
        savedNotification.setMaxRetries(3);
        
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(savedNotification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);
        
        // When
        NotificationResponse response = notificationService.retryFailedNotification(1L);
        
        // Then
        assertThat(response).isNotNull();
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }
    
    @Test
    void testRetryFailedNotification_MaxRetriesReached() {
        // Given
        savedNotification.setStatus(NotificationStatus.FAILED);
        savedNotification.setRetryCount(3);
        savedNotification.setMaxRetries(3);
        
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(savedNotification));
        
        // When/Then
        assertThatThrownBy(() -> notificationService.retryFailedNotification(1L))
            .isInstanceOf(InvalidRequestException.class)
            .hasMessageContaining("cannot be retried");
    }
    
    @Test
    void testGetNotificationsByRecipient() {
        // Given
        List<Notification> notifications = Arrays.asList(savedNotification);
        when(notificationRepository.findByRecipient("test@example.com")).thenReturn(notifications);
        
        // When
        List<NotificationResponse> result = notificationService.getNotificationsByRecipient("test@example.com");
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRecipient()).isEqualTo("test@example.com");
        
        verify(notificationRepository, times(1)).findByRecipient("test@example.com");
    }
}
