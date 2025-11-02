package com.Portfolio.Notifire.repository;

import com.Portfolio.Notifire.model.entity.Notification;
import com.Portfolio.Notifire.model.entity.Template;
import com.Portfolio.Notifire.model.enums.NotificationChannel;
import com.Portfolio.Notifire.model.enums.NotificationPriority;
import com.Portfolio.Notifire.model.enums.NotificationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test class for NotificationRepository
 */
@DataJpaTest
class NotificationRepositoryTest {
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    @Autowired
    private TemplateRepository templateRepository;
    
    @Test
    void testSaveNotification() {
        // Given
        Notification notification = new Notification();
        notification.setRecipient("test@example.com");
        notification.setSubject("Test Subject");
        notification.setContent("Test Content");
        notification.setChannel(NotificationChannel.EMAIL);
        notification.setStatus(NotificationStatus.PENDING);
        notification.setPriority(NotificationPriority.MEDIUM);
        
        // When
        Notification saved = notificationRepository.save(notification);
        
        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getRecipient()).isEqualTo("test@example.com");
        assertThat(saved.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(saved.getCreatedAt()).isNotNull();
    }
    
    @Test
    void testFindByStatus() {
        // Given
        Notification notification = new Notification();
        notification.setRecipient("test@example.com");
        notification.setSubject("Test");
        notification.setContent("Content");
        notification.setStatus(NotificationStatus.SENT);
        notificationRepository.save(notification);
        
        // When
        long count = notificationRepository.countByStatus(NotificationStatus.SENT);
        
        // Then
        assertThat(count).isGreaterThan(0);
    }
    
    @Test
    void testSaveTemplate() {
        // Given
        Template template = new Template();
        template.setName("welcome_email");
        template.setDescription("Welcome email template");
        template.setSubject("Welcome {{user_name}}!");
        template.setBody("<h1>Welcome {{user_name}}</h1>");
        template.setChannel(NotificationChannel.EMAIL);
        template.setIsActive(true);
        
        // When
        Template saved = templateRepository.save(template);
        
        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("welcome_email");
        assertThat(saved.isUsable()).isTrue();
    }
    
    @Test
    void testFindTemplateByName() {
        // Given
        Template template = new Template();
        template.setName("password_reset");
        template.setSubject("Reset Password");
        template.setBody("Reset your password");
        template.setChannel(NotificationChannel.EMAIL);
        templateRepository.save(template);
        
        // When
        var found = templateRepository.findByName("password_reset");
        
        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("password_reset");
    }
}
