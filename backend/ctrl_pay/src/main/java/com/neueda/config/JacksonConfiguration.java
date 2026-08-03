package com.neueda.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson configuration for JSON processing.
 * Provides ObjectMapper bean with proper configuration for handling Java Time types.
 */
@Configuration
public class JacksonConfiguration {

    /**
     * Creates and configures an ObjectMapper bean.
     * Includes support for Java 8 Date/Time API types.
     * 
     * @return Configured ObjectMapper instance
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // Register module for Java 8 date/time types (LocalDateTime, etc.)
        mapper.registerModule(new JavaTimeModule());
        // Prevent serialization of dates as timestamps
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}

