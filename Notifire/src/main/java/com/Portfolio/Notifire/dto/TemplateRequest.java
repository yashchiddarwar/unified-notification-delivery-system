package com.Portfolio.Notifire.dto;

import com.Portfolio.Notifire.model.enums.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO for creating/updating templates
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemplateRequest {
    
    @NotBlank(message = "Template name is required")
    @Size(max = 255, message = "Template name too long")
    private String name;
    
    @Size(max = 1000, message = "Description too long")
    private String description;
    
    @NotBlank(message = "Subject is required")
    @Size(max = 500, message = "Subject too long")
    private String subject;
    
    @NotBlank(message = "Body is required")
    private String body;
    
    /**
     * List of variable names expected in the template
     * e.g., ["user_name", "activation_link"]
     */
    private List<String> variables = new ArrayList<>();
    
    /**
     * Target channel (defaults to EMAIL)
     */
    private NotificationChannel channel = NotificationChannel.EMAIL;
    
    /**
     * Is template active? (defaults to true)
     */
    private Boolean isActive = true;
}
