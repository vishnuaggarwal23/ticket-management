package com.ticketmanagement.config;

import com.ticketmanagement.domain.TicketPriority;
import com.ticketmanagement.dto.serde.TicketPriorityJsonDeserializer;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.module.SimpleModule;

@Configuration
public class JacksonConfig {

    @Bean
    JsonMapperBuilderCustomizer ticketPriorityDeserializer() {
        return builder -> {
            SimpleModule module = new SimpleModule();
            module.addDeserializer(TicketPriority.class, new TicketPriorityJsonDeserializer());
            builder.addModule(module);
            builder.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        };
    }
}
