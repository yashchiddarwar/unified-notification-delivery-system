package com.Portfolio.Notifire.service;

import com.Portfolio.Notifire.dto.TemplateRequest;
import com.Portfolio.Notifire.dto.TemplateResponse;
import com.Portfolio.Notifire.exception.InvalidRequestException;
import com.Portfolio.Notifire.exception.TemplateNotFoundException;
import com.Portfolio.Notifire.exception.TemplateProcessingException;
import com.Portfolio.Notifire.model.entity.Template;
import com.Portfolio.Notifire.model.enums.NotificationChannel;
import com.Portfolio.Notifire.repository.TemplateRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service for managing templates
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TemplateService {
    
    private final TemplateRepository templateRepository;
    private final ObjectMapper objectMapper;
    
    private static final Pattern TEMPLATE_VARIABLE_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");
    
    /**
     * Create a new template
     */
    @Transactional
    public TemplateResponse createTemplate(TemplateRequest request) {
        log.debug("Creating template: {}", request.getName());
        
        // Check for duplicate name
        if (templateRepository.existsByName(request.getName())) {
            throw new InvalidRequestException("Template with name '" + request.getName() + "' already exists");
        }
        
        Template template = new Template();
        template.setName(request.getName());
        template.setDescription(request.getDescription());
        template.setSubject(request.getSubject());
        template.setBody(request.getBody());
        template.setChannel(request.getChannel());
        template.setIsActive(request.getIsActive());
        template.setVersion(1);
        
        // Convert variables list to JSON
        try {
            String variablesJson = objectMapper.writeValueAsString(request.getVariables());
            template.setVariables(variablesJson);
        } catch (JsonProcessingException e) {
            throw new TemplateProcessingException("Failed to process template variables", e);
        }
        
        Template saved = templateRepository.save(template);
        
        log.info("Template created with id: {} and name: {}", saved.getId(), saved.getName());
        
        return mapToResponse(saved);
    }
    
    /**
     * Get template by ID
     */
    public TemplateResponse getTemplateById(Long id) {
        log.debug("Fetching template with id: {}", id);
        
        Template template = templateRepository.findById(id)
            .orElseThrow(() -> new TemplateNotFoundException(id));
        
        return mapToResponse(template);
    }
    
    /**
     * Get template by name
     */
    public TemplateResponse getTemplateByName(String name) {
        log.debug("Fetching template with name: {}", name);
        
        Template template = templateRepository.findByName(name)
            .orElseThrow(() -> new TemplateNotFoundException(name));
        
        return mapToResponse(template);
    }
    
    /**
     * Get all templates
     */
    public List<TemplateResponse> getAllTemplates() {
        log.debug("Fetching all templates");
        
        return templateRepository.findAll()
            .stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get all active templates
     */
    public List<TemplateResponse> getActiveTemplates() {
        log.debug("Fetching active templates");
        
        return templateRepository.findByIsActiveTrue()
            .stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get templates by channel
     */
    public List<TemplateResponse> getTemplatesByChannel(NotificationChannel channel) {
        log.debug("Fetching templates for channel: {}", channel);
        
        return templateRepository.findByChannelAndIsActiveTrue(channel)
            .stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Update an existing template
     */
    @Transactional
    public TemplateResponse updateTemplate(Long id, TemplateRequest request) {
        log.debug("Updating template with id: {}", id);
        
        Template template = templateRepository.findById(id)
            .orElseThrow(() -> new TemplateNotFoundException(id));
        
        // Check for duplicate name (excluding current template)
        if (templateRepository.existsByNameAndIdNot(request.getName(), id)) {
            throw new InvalidRequestException("Template with name '" + request.getName() + "' already exists");
        }
        
        template.setName(request.getName());
        template.setDescription(request.getDescription());
        template.setSubject(request.getSubject());
        template.setBody(request.getBody());
        template.setChannel(request.getChannel());
        template.setIsActive(request.getIsActive());
        template.setVersion(template.getVersion() + 1);
        
        // Update variables
        try {
            String variablesJson = objectMapper.writeValueAsString(request.getVariables());
            template.setVariables(variablesJson);
        } catch (JsonProcessingException e) {
            throw new TemplateProcessingException("Failed to process template variables", e);
        }
        
        Template updated = templateRepository.save(template);
        
        log.info("Template updated: {} (version {})", updated.getName(), updated.getVersion());
        
        return mapToResponse(updated);
    }
    
    /**
     * Delete a template (soft delete - set inactive)
     */
    @Transactional
    public void deleteTemplate(Long id) {
        log.debug("Deleting template with id: {}", id);
        
        Template template = templateRepository.findById(id)
            .orElseThrow(() -> new TemplateNotFoundException(id));
        
        template.setIsActive(false);
        templateRepository.save(template);
        
        log.info("Template deactivated: {}", template.getName());
    }
    
    /**
     * Render template with variables
     * Replaces {{variable}} with actual values
     */
    public String renderTemplate(String template, Map<String, Object> variables) {
        if (template == null || template.isBlank()) {
            return template;
        }
        
        if (variables == null || variables.isEmpty()) {
            return template;
        }
        
        String result = template;
        Matcher matcher = TEMPLATE_VARIABLE_PATTERN.matcher(template);
        
        while (matcher.find()) {
            String variableName = matcher.group(1).trim();
            Object value = variables.get(variableName);
            
            if (value != null) {
                result = result.replace("{{" + matcher.group(1) + "}}", value.toString());
            } else {
                log.warn("Variable '{}' not found in provided variables", variableName);
            }
        }
        
        return result;
    }
    
    /**
     * Map entity to response DTO
     */
    private TemplateResponse mapToResponse(Template template) {
        List<String> variablesList = null;
        
        if (template.getVariables() != null) {
            try {
                variablesList = objectMapper.readValue(
                    template.getVariables(), 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
                );
            } catch (JsonProcessingException e) {
                log.error("Failed to parse template variables for template: {}", template.getId(), e);
            }
        }
        
        return TemplateResponse.builder()
            .id(template.getId())
            .name(template.getName())
            .description(template.getDescription())
            .subject(template.getSubject())
            .body(template.getBody())
            .variables(variablesList)
            .channel(template.getChannel())
            .isActive(template.getIsActive())
            .version(template.getVersion())
            .createdAt(template.getCreatedAt())
            .updatedAt(template.getUpdatedAt())
            .build();
    }
}
