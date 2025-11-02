package com.Portfolio.Notifire.dto;

import com.Portfolio.Notifire.model.enums.NotificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for notification response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {
    
    private Long id;
    
    private String recipient;
    
    private String subject;
    
    private NotificationStatus status;
    
    private String message;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime sentAt;
    
    private LocalDateTime scheduledAt;
    
    private Integer retryCount;
    
    private String errorMessage;
}
