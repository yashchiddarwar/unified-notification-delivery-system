package com.Portfolio.Notifire.service;

import com.Portfolio.Notifire.dto.TemplateRequest;
import com.Portfolio.Notifire.dto.TemplateResponse;
import com.Portfolio.Notifire.exception.InvalidRequestException;
import com.Portfolio.Notifire.exception.TemplateNotFoundException;
import com.Portfolio.Notifire.model.entity.Template;
import com.Portfolio.Notifire.model.enums.NotificationChannel;
import com.Portfolio.Notifire.repository.TemplateRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TemplateService
 */
@ExtendWith(MockitoExtension.class)
class TemplateServiceTest {
    
    @Mock
    private TemplateRepository templateRepository;
    
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();
    
    @InjectMocks
    private TemplateService templateService;
    
    private TemplateRequest validRequest;
    private Template savedTemplate;
    
    @BeforeEach
    void setUp() throws Exception {
        validRequest = new TemplateRequest();
        validRequest.setName("welcome_email");
        validRequest.setDescription("Welcome email template");
        validRequest.setSubject("Welcome {{user_name}}!");
        validRequest.setBody("Hello {{user_name}}, welcome to our service!");
        validRequest.setVariables(Arrays.asList("user_name"));
        validRequest.setChannel(NotificationChannel.EMAIL);
        validRequest.setIsActive(true);
        
        savedTemplate = new Template();
        savedTemplate.setId(1L);
        savedTemplate.setName("welcome_email");
        savedTemplate.setDescription("Welcome email template");
        savedTemplate.setSubject("Welcome {{user_name}}!");
        savedTemplate.setBody("Hello {{user_name}}, welcome to our service!");
        savedTemplate.setVariables("[\"user_name\"]");
        savedTemplate.setChannel(NotificationChannel.EMAIL);
        savedTemplate.setIsActive(true);
        savedTemplate.setVersion(1);
    }
    
    @Test
    void testCreateTemplate_Success() throws Exception {
        // Given
        when(templateRepository.existsByName("welcome_email")).thenReturn(false);
        when(templateRepository.save(any(Template.class))).thenReturn(savedTemplate);
        
        // When
        TemplateResponse response = templateService.createTemplate(validRequest);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("welcome_email");
        assertThat(response.getVersion()).isEqualTo(1);
        
        verify(templateRepository, times(1)).existsByName("welcome_email");
        verify(templateRepository, times(1)).save(any(Template.class));
    }
    
    @Test
    void testCreateTemplate_DuplicateName() {
        // Given
        when(templateRepository.existsByName("welcome_email")).thenReturn(true);
        
        // When/Then
        assertThatThrownBy(() -> templateService.createTemplate(validRequest))
            .isInstanceOf(InvalidRequestException.class)
            .hasMessageContaining("already exists");
        
        verify(templateRepository, never()).save(any(Template.class));
    }
    
    @Test
    void testGetTemplateById_Found() {
        // Given
        when(templateRepository.findById(1L)).thenReturn(Optional.of(savedTemplate));
        
        // When
        TemplateResponse response = templateService.getTemplateById(1L);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("welcome_email");
        
        verify(templateRepository, times(1)).findById(1L);
    }
    
    @Test
    void testGetTemplateById_NotFound() {
        // Given
        when(templateRepository.findById(999L)).thenReturn(Optional.empty());
        
        // When/Then
        assertThatThrownBy(() -> templateService.getTemplateById(999L))
            .isInstanceOf(TemplateNotFoundException.class);
    }
    
    @Test
    void testGetTemplateByName_Found() {
        // Given
        when(templateRepository.findByName("welcome_email")).thenReturn(Optional.of(savedTemplate));
        
        // When
        TemplateResponse response = templateService.getTemplateByName("welcome_email");
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("welcome_email");
        
        verify(templateRepository, times(1)).findByName("welcome_email");
    }
    
    @Test
    void testGetAllTemplates() {
        // Given
        List<Template> templates = Arrays.asList(savedTemplate);
        when(templateRepository.findAll()).thenReturn(templates);
        
        // When
        List<TemplateResponse> result = templateService.getAllTemplates();
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        
        verify(templateRepository, times(1)).findAll();
    }
    
    @Test
    void testGetActiveTemplates() {
        // Given
        List<Template> templates = Arrays.asList(savedTemplate);
        when(templateRepository.findByIsActiveTrue()).thenReturn(templates);
        
        // When
        List<TemplateResponse> result = templateService.getActiveTemplates();
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIsActive()).isTrue();
        
        verify(templateRepository, times(1)).findByIsActiveTrue();
    }
    
    @Test
    void testGetTemplatesByChannel() {
        // Given
        List<Template> templates = Arrays.asList(savedTemplate);
        when(templateRepository.findByChannelAndIsActiveTrue(NotificationChannel.EMAIL)).thenReturn(templates);
        
        // When
        List<TemplateResponse> result = templateService.getTemplatesByChannel(NotificationChannel.EMAIL);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getChannel()).isEqualTo(NotificationChannel.EMAIL);
        
        verify(templateRepository, times(1)).findByChannelAndIsActiveTrue(NotificationChannel.EMAIL);
    }
    
    @Test
    void testUpdateTemplate_Success() throws Exception {
        // Given
        when(templateRepository.findById(1L)).thenReturn(Optional.of(savedTemplate));
        when(templateRepository.existsByNameAndIdNot("welcome_email", 1L)).thenReturn(false);
        when(templateRepository.save(any(Template.class))).thenReturn(savedTemplate);
        
        // When
        TemplateResponse response = templateService.updateTemplate(1L, validRequest);
        
        // Then
        assertThat(response).isNotNull();
        verify(templateRepository, times(1)).save(any(Template.class));
    }
    
    @Test
    void testUpdateTemplate_NotFound() {
        // Given
        when(templateRepository.findById(999L)).thenReturn(Optional.empty());
        
        // When/Then
        assertThatThrownBy(() -> templateService.updateTemplate(999L, validRequest))
            .isInstanceOf(TemplateNotFoundException.class);
    }
    
    @Test
    void testUpdateTemplate_DuplicateName() {
        // Given
        when(templateRepository.findById(1L)).thenReturn(Optional.of(savedTemplate));
        when(templateRepository.existsByNameAndIdNot("welcome_email", 1L)).thenReturn(true);
        
        // When/Then
        assertThatThrownBy(() -> templateService.updateTemplate(1L, validRequest))
            .isInstanceOf(InvalidRequestException.class)
            .hasMessageContaining("already exists");
    }
    
    @Test
    void testDeleteTemplate() {
        // Given
        when(templateRepository.findById(1L)).thenReturn(Optional.of(savedTemplate));
        when(templateRepository.save(any(Template.class))).thenReturn(savedTemplate);
        
        // When
        templateService.deleteTemplate(1L);
        
        // Then
        verify(templateRepository, times(1)).save(any(Template.class));
    }
    
    @Test
    void testRenderTemplate_WithVariables() {
        // Given
        String template = "Hello {{user_name}}, your order {{order_id}} is ready!";
        Map<String, Object> variables = new HashMap<>();
        variables.put("user_name", "John Doe");
        variables.put("order_id", "12345");
        
        // When
        String result = templateService.renderTemplate(template, variables);
        
        // Then
        assertThat(result).isEqualTo("Hello John Doe, your order 12345 is ready!");
    }
    
    @Test
    void testRenderTemplate_MissingVariable() {
        // Given
        String template = "Hello {{user_name}}, your order {{order_id}} is ready!";
        Map<String, Object> variables = new HashMap<>();
        variables.put("user_name", "John Doe");
        // order_id is missing
        
        // When
        String result = templateService.renderTemplate(template, variables);
        
        // Then
        assertThat(result).contains("John Doe");
        assertThat(result).contains("{{order_id}}"); // Placeholder remains
    }
    
    @Test
    void testRenderTemplate_NoVariables() {
        // Given
        String template = "Hello World!";
        
        // When
        String result = templateService.renderTemplate(template, null);
        
        // Then
        assertThat(result).isEqualTo("Hello World!");
    }
    
    @Test
    void testRenderTemplate_EmptyTemplate() {
        // Given
        String template = "";
        Map<String, Object> variables = new HashMap<>();
        
        // When
        String result = templateService.renderTemplate(template, variables);
        
        // Then
        assertThat(result).isEmpty();
    }
}
