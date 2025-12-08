package com.gdn.project.waroenk.member.configuration;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Jackson configuration for proper Java 8 date/time serialization.
 */
@Configuration
public class JacksonConfiguration {

    /**
     * Primary ObjectMapper for REST APIs (no type info needed)
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        
        // Register Java 8 date/time module
        objectMapper.registerModule(new JavaTimeModule());
        
        // Write dates as ISO-8601 strings instead of timestamps
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        return objectMapper;
    }

    /**
     * ObjectMapper for Redis serialization (includes type info for proper deserialization)
     */
    @Bean("redisObjectMapper")
    public ObjectMapper redisObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        
        // Register Java 8 date/time module
        objectMapper.registerModule(new JavaTimeModule());
        
        // Write dates as ISO-8601 strings
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // Configure type validator to allow our package classes
        PolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
            .allowIfBaseType(Object.class)
            .allowIfSubType("com.gdn.project.waroenk")
            .allowIfSubType("java.")
            .build();
        
        // Enable type information for proper deserialization
        // This adds @class property to JSON so Jackson knows which class to instantiate
        objectMapper.activateDefaultTyping(
            typeValidator,
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY
        );
        
        return objectMapper;
    }
}
