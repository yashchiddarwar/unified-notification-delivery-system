package com.Portfolio.Notifire.config;

import com.sendgrid.SendGrid;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SendGrid configuration
 */
@Configuration
@ConfigurationProperties(prefix = "sendgrid")
@Getter
@Setter
public class SendGridConfig {
    
    private String apiKey;
    private String fromEmail;
    private String fromName;
    private boolean enabled = false;
    
    @Bean
    public SendGrid sendGrid() {
        return new SendGrid(apiKey);
    }
}
