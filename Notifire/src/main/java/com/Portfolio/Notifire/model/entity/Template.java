package com.Portfolio.Notifire.model.entity;

import com.Portfolio.Notifire.model.enums.NotificationChannel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing an email/notification template.
 */
@Entity
@Table(name = "templates", indexes = {
    @Index(name = "idx_name", columnList = "name"),
    @Index(name = "idx_channel", columnList = "channel")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Template {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 255)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(nullable = false, length = 500)
    private String subject;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;
    
    /**
     * JSON string containing variable definitions
     * Example: {"user_name": "string", "company_name": "string"}
     */
    @Column(columnDefinition = "TEXT")
    private String variables;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 50, nullable = false)
    private NotificationChannel channel = NotificationChannel.EMAIL;
    
    @Column(nullable = false)
    private Boolean isActive = true;
    
    @Column(nullable = false)
    private Integer version = 1;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    /**
     * Helper method to check if template is usable
     */
    public boolean isUsable() {
        return isActive != null && isActive;
    }
}
