package com.Portfolio.Notifire.integration;

import com.Portfolio.Notifire.dto.NotificationRequest;
import com.Portfolio.Notifire.dto.NotificationResponse;
import com.Portfolio.Notifire.dto.TemplateRequest;
import com.Portfolio.Notifire.dto.TemplateResponse;
import com.Portfolio.Notifire.model.enums.NotificationChannel;
import com.Portfolio.Notifire.model.enums.NotificationPriority;
import com.Portfolio.Notifire.model.enums.NotificationStatus;
import com.Portfolio.Notifire.service.NotificationService;
import com.Portfolio.Notifire.service.TemplateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for notification flow
 */
@SpringBootTest
@ActiveProfiles("test")
class NotificationIntegrationTest {
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private TemplateService templateService;
    
    @Test
    void testSendNotification_WithoutTemplate() throws InterruptedException {
        // Given
        NotificationRequest request = new NotificationRequest();
        request.setRecipient("user@example.com");
        request.setSubject("Test Notification");
        request.setContent("This is a test notification");
        request.setPriority(NotificationPriority.HIGH);
        request.setChannel(NotificationChannel.EMAIL);
        
        // When
        NotificationResponse response = notificationService.sendNotification(request);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isNotNull();
        assertThat(response.getRecipient()).isEqualTo("user@example.com");
        assertThat(response.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(response.getMessage()).contains("queued successfully");
        
        // Wait for async processing
        Thread.sleep(2000);
        
        // Verify notification was processed
        NotificationResponse updated = notificationService.getNotificationById(response.getId());
        assertThat(updated.getStatus()).isIn(NotificationStatus.SENT, NotificationStatus.SENDING);
    }
    
    @Test
    void testSendNotification_WithTemplate() throws InterruptedException {
        // Given - Create template first
        TemplateRequest templateRequest = new TemplateRequest();
        templateRequest.setName("integration_test_template");
        templateRequest.setSubject("Welcome {{user_name}}!");
        templateRequest.setBody("Hello {{user_name}}, your account {{account_id}} is ready!");
        templateRequest.setVariables(Arrays.asList("user_name", "account_id"));
        templateRequest.setChannel(NotificationChannel.EMAIL);
        
        TemplateResponse template = templateService.createTemplate(templateRequest);
        
        // Create notification with template
        NotificationRequest notificationRequest = new NotificationRequest();
        notificationRequest.setRecipient("newuser@example.com");
        notificationRequest.setTemplateId(template.getId());
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("user_name", "John Doe");
        variables.put("account_id", "ACC12345");
        notificationRequest.setVariables(variables);
        
        // When
        NotificationResponse response = notificationService.sendNotification(notificationRequest);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isNotNull();
        assertThat(response.getSubject()).isEqualTo("Welcome John Doe!");
        
        // Wait for async processing
        Thread.sleep(2000);
        
        // Verify notification was processed
        NotificationResponse updated = notificationService.getNotificationById(response.getId());
        assertThat(updated.getStatus()).isIn(NotificationStatus.SENT, NotificationStatus.SENDING);
    }
}
