package dev.sunbirdrc.validators.json.jsonschema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.sunbirdrc.pojos.RegistryLookup;
import dev.sunbirdrc.registry.middleware.MiddlewareHaltException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class XValidationServiceTest {

    @Mock
    private RegistryLookup registryLookup;

    private XValidationService xValidationService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        xValidationService = new XValidationService(registryLookup);
        objectMapper = new ObjectMapper();
    }

    @Test
    void shouldPassWhenNoXValidationRules() throws Exception {
        // Given
        String schema = "{\n" +
                "    \"type\": \"object\",\n" +
                "    \"properties\": {\n" +
                "        \"name\": { \"type\": \"string\" }\n" +
                "    }\n" +
                "}";
        String data = "{\n" +
                "    \"name\": \"John Doe\"\n" +
                "}";

        Schema schemaObj = SchemaLoader.builder()
                .schemaJson(new JSONObject(schema))
                .build()
                .load()
                .build();
        JSONObject dataObj = new JSONObject(data);

        // When
        xValidationService.validate(schemaObj, dataObj);

        // Then
        // No exception should be thrown
    }

    @Test
    void shouldPassWhenEqualityRuleIsValid() throws Exception {
        // Given
        String schema = "{\n" +
                "    \"type\": \"object\",\n" +
                "    \"properties\": {\n" +
                "        \"password\": { \"type\": \"string\" },\n" +
                "        \"confirmPassword\": { \"type\": \"string\" }\n" +
                "    },\n" +
                "    \"x-validation\": {\n" +
                "        \"passwordMatch\": {\n" +
                "            \"rule\": \"password==confirmPassword\",\n" +
                "            \"description\": \"Passwords must match\"\n" +
                "        }\n" +
                "    }\n" +
                "}";
        String data = "{\n" +
                "    \"password\": \"secret123\",\n" +
                "    \"confirmPassword\": \"secret123\"\n" +
                "}";

        Schema schemaObj = SchemaLoader.builder()
                .schemaJson(new JSONObject(schema))
                .build()
                .load()
                .build();
        JSONObject dataObj = new JSONObject(data);

        // When
        xValidationService.validate(schemaObj, dataObj);

        // Then
        // No exception should be thrown
    }

    @Test
    void shouldFailWhenEqualityRuleIsInvalid() {
        // Given
        String schema = "{\n" +
                "    \"type\": \"object\",\n" +
                "    \"properties\": {\n" +
                "        \"password\": { \"type\": \"string\" },\n" +
                "        \"confirmPassword\": { \"type\": \"string\" }\n" +
                "    },\n" +
                "    \"x-validation\": {\n" +
                "        \"passwordMatch\": {\n" +
                "            \"rule\": \"password==confirmPassword\",\n" +
                "            \"description\": \"Passwords must match\"\n" +
                "        }\n" +
                "    }\n" +
                "}";
        String data = "{\n" +
                "    \"password\": \"secret123\",\n" +
                "    \"confirmPassword\": \"different\"\n" +
                "}";

        Schema schemaObj = SchemaLoader.builder()
                .schemaJson(new JSONObject(schema))
                .build()
                .load()
                .build();
        JSONObject dataObj = new JSONObject(data);

        // When & Then
        assertThrows(MiddlewareHaltException.class, () -> {
            xValidationService.validate(schemaObj, dataObj);
        });
    }

    @Test
    void shouldPassWhenRegistryExistenceRuleIsValid() throws Exception {
        // Given
        String schema = "{\n" +
                "    \"type\": \"object\",\n" +
                "    \"properties\": {\n" +
                "        \"email\": { \"type\": \"string\" }\n" +
                "    },\n" +
                "    \"x-validation\": {\n" +
                "        \"emailExists\": {\n" +
                "            \"rule\": \"existsInRegistry('User', 'email', email)\",\n" +
                "            \"description\": \"Email must exist in registry\"\n" +
                "        }\n" +
                "    }\n" +
                "}";
        String data = "{\n" +
                "    \"email\": \"test@example.com\"\n" +
                "}";

        Schema schemaObj = SchemaLoader.builder()
                .schemaJson(new JSONObject(schema))
                .build()
                .load()
                .build();
        JSONObject dataObj = new JSONObject(data);

        when(registryLookup.exists(anyString(), anyString(), anyString())).thenReturn(true);

        // When
        xValidationService.validate(schemaObj, dataObj);

        // Then
        // No exception should be thrown
    }

    @Test
    void shouldFailWhenRegistryExistenceRuleIsInvalid() {
        // Given
        String schema = "{\n" +
                "    \"type\": \"object\",\n" +
                "    \"properties\": {\n" +
                "        \"email\": { \"type\": \"string\" }\n" +
                "    },\n" +
                "    \"x-validation\": {\n" +
                "        \"emailExists\": {\n" +
                "            \"rule\": \"existsInRegistry('User', 'email', email)\",\n" +
                "            \"description\": \"Email must exist in registry\"\n" +
                "        }\n" +
                "    }\n" +
                "}";
        String data = "{\n" +
                "    \"email\": \"nonexistent@example.com\"\n" +
                "}";

        Schema schemaObj = SchemaLoader.builder()
                .schemaJson(new JSONObject(schema))
                .build()
                .load()
                .build();
        JSONObject dataObj = new JSONObject(data);

        when(registryLookup.exists(anyString(), anyString(), anyString())).thenReturn(false);

        // When & Then
        assertThrows(MiddlewareHaltException.class, () -> {
            xValidationService.validate(schemaObj, dataObj);
        });
    }

    @Test
    void shouldPassWhenRegistryUniquenessRuleIsValid() throws Exception {
        // Given
        String schema = "{\n" +
                "    \"type\": \"object\",\n" +
                "    \"properties\": {\n" +
                "        \"email\": { \"type\": \"string\" },\n" +
                "        \"phone\": { \"type\": \"string\" }\n" +
                "    },\n" +
                "    \"x-validation\": {\n" +
                "        \"uniqueUser\": {\n" +
                "            \"rule\": \"isUniqueInRegistry('User', {'email': email, 'phone': phone})\",\n" +
                "            \"description\": \"User must be unique\"\n" +
                "        }\n" +
                "    }\n" +
                "}";
        String data = "{\n" +
                "    \"email\": \"test@example.com\",\n" +
                "    \"phone\": \"1234567890\"\n" +
                "}";

        Schema schemaObj = SchemaLoader.builder()
                .schemaJson(new JSONObject(schema))
                .build()
                .load()
                .build();
        JSONObject dataObj = new JSONObject(data);

        when(registryLookup.isUnique(anyString(), any())).thenReturn(true);

        // When
        xValidationService.validate(schemaObj, dataObj);

        // Then
        // No exception should be thrown
    }

    @Test
    void shouldFailWhenRegistryUniquenessRuleIsInvalid() {
        // Given
        String schema = "{\n" +
                "    \"type\": \"object\",\n" +
                "    \"properties\": {\n" +
                "        \"email\": { \"type\": \"string\" },\n" +
                "        \"phone\": { \"type\": \"string\" }\n" +
                "    },\n" +
                "    \"x-validation\": {\n" +
                "        \"uniqueUser\": {\n" +
                "            \"rule\": \"isUniqueInRegistry('User', {'email': email, 'phone': phone})\",\n" +
                "            \"description\": \"User must be unique\"\n" +
                "        }\n" +
                "    }\n" +
                "}";
        String data = "{\n" +
                "    \"email\": \"existing@example.com\",\n" +
                "    \"phone\": \"1234567890\"\n" +
                "}";

        Schema schemaObj = SchemaLoader.builder()
                .schemaJson(new JSONObject(schema))
                .build()
                .load()
                .build();
        JSONObject dataObj = new JSONObject(data);

        when(registryLookup.isUnique(anyString(), any())).thenReturn(false);

        // When & Then
        assertThrows(MiddlewareHaltException.class, () -> {
            xValidationService.validate(schemaObj, dataObj);
        });
    }

    @Test
    void shouldHandleConcatenatedValuesInEqualityRule() throws Exception {
        // Given
        String schema = "{\n" +
                "    \"type\": \"object\",\n" +
                "    \"properties\": {\n" +
                "        \"firstName\": { \"type\": \"string\" },\n" +
                "        \"lastName\": { \"type\": \"string\" },\n" +
                "        \"fullName\": { \"type\": \"string\" }\n" +
                "    },\n" +
                "    \"x-validation\": {\n" +
                "        \"nameMatch\": {\n" +
                "            \"rule\": \"fullName==firstName+lastName\",\n" +
                "            \"description\": \"Full name must match concatenated first and last name\"\n" +
                "        }\n" +
                "    }\n" +
                "}";
        String data = "{\n" +
                "    \"firstName\": \"John\",\n" +
                "    \"lastName\": \"Doe\",\n" +
                "    \"fullName\": \"JohnDoe\"\n" +
                "}";

        Schema schemaObj = SchemaLoader.builder()
                .schemaJson(new JSONObject(schema))
                .build()
                .load()
                .build();
        JSONObject dataObj = new JSONObject(data);

        // When
        xValidationService.validate(schemaObj, dataObj);

        // Then
        // No exception should be thrown
    }
} 