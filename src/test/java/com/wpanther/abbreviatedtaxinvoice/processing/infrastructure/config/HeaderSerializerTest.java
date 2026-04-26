package com.wpanther.abbreviatedtaxinvoice.processing.infrastructure.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HeaderSerializerTest {

    private HeaderSerializer serializer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        serializer = new HeaderSerializer(objectMapper);
    }

    @Test
    void toJson_returnsValidJsonString() {
        Map<String, String> headers = Map.of("sagaId", "saga-1", "correlationId", "corr-1", "status", "SUCCESS");

        String json = serializer.toJson(headers);

        assertNotNull(json);
        assertTrue(json.contains("saga-1"));
        assertTrue(json.contains("corr-1"));
        assertTrue(json.contains("SUCCESS"));
    }

    @Test
    void toJson_succeedsForEmptyMap() {
        String json = serializer.toJson(Map.of());

        assertNotNull(json);
        assertEquals("{}", json);
    }

    @Test
    void toJson_throwsIllegalStateExceptionOnFailure() {
        ObjectMapper failingMapper = new ObjectMapper() {
            @Override
            public String writeValueAsString(Object value) throws JsonProcessingException {
                throw new JsonProcessingException("serialization error") {};
            }
        };
        HeaderSerializer failingSerializer = new HeaderSerializer(failingMapper);

        assertThrows(IllegalStateException.class, () -> failingSerializer.toJson(Map.of("key", "value")));
    }
}