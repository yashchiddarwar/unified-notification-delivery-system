package com.Portfolio.Notifire.dto;

import com.Portfolio.Notifire.model.enums.NotificationChannel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for template response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateResponse {
    
    private Long id;
    
    private String name;
    
    private String description;
    
    private String subject;
    
    private String body;
    
    private List<String> variables;
    
    private NotificationChannel channel;
    
    private Boolean isActive;
    
    private Integer version;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}
