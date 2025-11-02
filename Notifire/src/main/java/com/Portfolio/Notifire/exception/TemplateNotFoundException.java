package com.Portfolio.Notifire.exception;

/**
 * Exception thrown when a template is not found
 */
public class TemplateNotFoundException extends RuntimeException {
    
    public TemplateNotFoundException(Long id) {
        super("Template not found with id: " + id);
    }
    
    public TemplateNotFoundException(String name) {
        super("Template not found with name: " + name);
    }
    
    public TemplateNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
