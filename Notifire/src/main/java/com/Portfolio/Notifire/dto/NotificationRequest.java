package com.Portfolio.Notifire.dto;

import com.Portfolio.Notifire.model.enums.NotificationChannel;
import com.Portfolio.Notifire.model.enums.NotificationPriority;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * DTO for creating notification requests
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {
    
    @NotBlank(message = "Recipient is required")
    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Recipient email too long")
    private String recipient;
    
    @Size(max = 500, message = "Subject too long")
    private String subject;
    
    @Size(max = 5000, message = "Content too long")
    private String content;
    
    /**
     * Optional: Use template instead of direct content
     */
    private Long templateId;
    
    /**
     * Variables to replace in template (e.g., {{user_name}})
     */
    private Map<String, Object> variables = new HashMap<>();
    
    /**
     * Notification priority (defaults to MEDIUM)
     */
    private NotificationPriority priority = NotificationPriority.MEDIUM;
    
    /**
     * Notification channel (defaults to EMAIL)
     */
    private NotificationChannel channel = NotificationChannel.EMAIL;
    
    /**
     * Optional: Schedule notification for future delivery
     */
    private LocalDateTime scheduledAt;
    
    /**
     * Validate that either content or templateId is provided
     */
    public boolean isValid() {
        return (content != null && !content.isBlank()) || templateId != null;
    }
}
