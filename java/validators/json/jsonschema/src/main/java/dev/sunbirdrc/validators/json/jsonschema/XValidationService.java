package dev.sunbirdrc.validators.json.jsonschema;

import com.fasterxml.jackson.databind.JsonNode;
import dev.sunbirdrc.registry.middleware.MiddlewareHaltException;
import dev.sunbirdrc.pojos.RegistryLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class XValidationService {
    private static final Logger logger = LoggerFactory.getLogger(XValidationService.class);
    private final RegistryLookup registryLookup;

    public XValidationService(RegistryLookup registryLookup) {
        this.registryLookup = registryLookup;
    }

    public void validate(JsonNode schemaNode, JsonNode dataNode) throws MiddlewareHaltException {
        if (!schemaNode.has("x-validation")) {
            return;
        }

        JsonNode xValidationNode = schemaNode.get("x-validation");
        Iterator<Map.Entry<String, JsonNode>> rules = xValidationNode.fields();

        while (rules.hasNext()) {
            Map.Entry<String, JsonNode> rule = rules.next();
            String ruleName = rule.getKey();
            JsonNode ruleNode = rule.getValue();

            String ruleExpression = ruleNode.get("rule").asText();
            String description = ruleNode.has("description") ? ruleNode.get("description").asText() : "";

            try {
                if (!validateRule(ruleExpression, dataNode)) {
                    throw new MiddlewareHaltException(String.format("X-Validation failed for rule '%s': %s", ruleName, description));
                }
            } catch (Exception e) {
                logger.error("Error validating rule {}: {}", ruleName, e.getMessage());
                throw new MiddlewareHaltException(String.format("X-Validation failed for rule '%s': %s", ruleName, e.getMessage()));
            }
        }
    }

    private boolean validateRule(String ruleExpression, JsonNode dataNode) throws Exception {
        if (ruleExpression.contains("existsInRegistry")) {
            return validateRegistryExistence(ruleExpression, dataNode);
        } else if (ruleExpression.contains("isUniqueInRegistry")) {
            return validateRegistryUniqueness(ruleExpression, dataNode);
        } else {
            return validateEquality(ruleExpression, dataNode);
        }
    }

    private boolean validateEquality(String ruleExpression, JsonNode dataNode) throws Exception {
        String[] parts = ruleExpression.split("==");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid equality rule format");
        }

        String leftExpr = parts[0].trim();
        String rightExpr = parts[1].trim();

        // Handle concatenation in right expression
        if (rightExpr.contains("+")) {
            String[] rightParts = rightExpr.split("\\+");
            StringBuilder concatenated = new StringBuilder();
            for (String part : rightParts) {
                String field = part.trim();
                if (dataNode.has(field)) {
                    concatenated.append(dataNode.get(field).asText());
                } else {
                    throw new IllegalArgumentException("Field not found: " + field);
                }
            }
            rightExpr = concatenated.toString();
        } else if (dataNode.has(rightExpr)) {
            // If right side is a field reference, get its value
            rightExpr = dataNode.get(rightExpr).asText();
        }

        String leftValue = dataNode.has(leftExpr) ? dataNode.get(leftExpr).asText() : "";
        return leftValue.equals(rightExpr);
    }

    private boolean validateRegistryExistence(String ruleExpression, JsonNode dataNode) throws Exception {
        // Parse rule: existsInRegistry('EntityType', 'field', valueField) or
        // existsInRegistry('EntityType', {'field1': value1, 'field2': value2})
        String[] parts = ruleExpression.split("'");
        if (parts.length < 3) {
            throw new IllegalArgumentException("Invalid registry existence rule format");
        }

        String entityType = parts[1];
        
        // Check if it's a single field or multiple fields
        if (parts[2].contains("{")) {
            // Multiple fields case
            String conditionsStr = parts[2].substring(parts[2].indexOf('{') + 1, parts[2].lastIndexOf('}')).trim();
            Map<String, String> conditions = parseConditions(conditionsStr, dataNode);
            return registryLookup.exists(entityType, conditions);
        } else {
            // Single field case
            String field = parts[3];
            String valueField = parts[4].substring(1, parts[4].length() - 1).trim();

            if (!dataNode.has(valueField)) {
                throw new IllegalArgumentException("Field not found: " + valueField);
            }

            String value = dataNode.get(valueField).asText();
            return registryLookup.exists(entityType, field, value);
        }
    }

    private boolean validateRegistryUniqueness(String ruleExpression, JsonNode dataNode) throws Exception {
        // Use regex to extract entity type and conditions map
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("^isUniqueInRegistry\\('([^']+)',\\s*\\{(.+)}\\)$");
        java.util.regex.Matcher matcher = pattern.matcher(ruleExpression);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Invalid registry uniqueness rule format");
        }
        String entityType = matcher.group(1);
        String conditionsStr = matcher.group(2).trim();
        Map<String, String> conditions = parseConditions(conditionsStr, dataNode);
        return registryLookup.isUnique(entityType, conditions);
    }

    private Map<String, String> parseConditions(String conditionsStr, JsonNode dataNode) throws Exception {
        Map<String, String> conditions = new HashMap<>();
        String[] conditionPairs = conditionsStr.split(",\\s*");  // Split by comma followed by optional whitespace
        for (String pair : conditionPairs) {
            String[] keyValue = pair.split(":\\s*");  // Split by colon followed by optional whitespace
            if (keyValue.length != 2) {
                throw new IllegalArgumentException("Invalid condition pair format: " + pair);
            }

            String field = keyValue[0].trim().replaceAll("'", "");
            String valueField = keyValue[1].trim();

            if (!dataNode.has(valueField)) {
                throw new IllegalArgumentException("Field not found: " + valueField);
            }

            conditions.put(field, dataNode.get(valueField).asText());
        }
        return conditions;
    }
} 