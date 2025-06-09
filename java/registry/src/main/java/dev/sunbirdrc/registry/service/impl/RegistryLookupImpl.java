package dev.sunbirdrc.registry.service.impl;

import dev.sunbirdrc.pojos.RegistryLookup;
import dev.sunbirdrc.registry.service.RegistryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class RegistryLookupImpl implements RegistryLookup {
    private static final Logger logger = LoggerFactory.getLogger(RegistryLookupImpl.class);
    private final RegistryService registryService;

    public RegistryLookupImpl(RegistryService registryService) {
        this.registryService = registryService;
    }

    @Override
    public boolean exists(String entityType, Map<String, String> conditions) {
        try {
            return registryService.exists(entityType, conditions);
        } catch (Exception e) {
            logger.error("Error checking existence in registry for entity type {} with conditions {}: {}", 
                entityType, conditions, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean isUnique(String entityType, Map<String, String> conditions) {
        try {
            return registryService.isUnique(entityType, conditions);
        } catch (Exception e) {
            logger.error("Error checking uniqueness in registry for entity type {} with conditions {}: {}", 
                entityType, conditions, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean exists(String entityType, String field, String value) {
        try {
            return registryService.exists(entityType, field, value);
        } catch (Exception e) {
            logger.error("Error checking existence in registry for entity type {} with field {} and value {}: {}", 
                entityType, field, value, e.getMessage());
            return false;
        }
    }
} 