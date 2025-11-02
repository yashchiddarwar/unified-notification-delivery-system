package com.Portfolio.Notifire.repository;

import com.Portfolio.Notifire.model.entity.Template;
import com.Portfolio.Notifire.model.enums.NotificationChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Template entity.
 */
@Repository
public interface TemplateRepository extends JpaRepository<Template, Long> {
    
    /**
     * Find template by name
     */
    Optional<Template> findByName(String name);
    
    /**
     * Find all active templates
     */
    List<Template> findByIsActiveTrue();
    
    /**
     * Find templates by channel
     */
    List<Template> findByChannel(NotificationChannel channel);
    
    /**
     * Find active templates by channel
     */
    List<Template> findByChannelAndIsActiveTrue(NotificationChannel channel);
    
    /**
     * Check if template name already exists
     */
    boolean existsByName(String name);
    
    /**
     * Check if template name exists excluding current id
     */
    boolean existsByNameAndIdNot(String name, Long id);
}
