package com.ticketmanagement.config;

import com.ticketmanagement.api.ticket.TicketPriorityJsonDeserializer;
import com.ticketmanagement.domain.TicketPriority;
import com.fasterxml.jackson.databind.DeserializationFeature;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer ticketPriorityDeserializer() {
        return builder -> builder
                .deserializerByType(TicketPriority.class, new TicketPriorityJsonDeserializer())
                .featuresToEnable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }
}
