package com.bidb.personetakip.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Configuration
public class JacksonConfig {

    private static final String DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    private static final ZoneId TURKEY_ZONE = ZoneId.of("Europe/Istanbul");

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // Register JavaTimeModule for LocalDateTime support
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        
        // Custom LocalDateTime serializer that ensures Turkey timezone
        javaTimeModule.addSerializer(LocalDateTime.class, 
            new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DATETIME_FORMAT)));
        
        mapper.registerModule(javaTimeModule);
        
        // Set default timezone to Turkey
        mapper.setTimeZone(java.util.TimeZone.getTimeZone(TURKEY_ZONE));
        
        return mapper;
    }
}