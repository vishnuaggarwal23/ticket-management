package com.ticketmanagement.config;

import com.ticketmanagement.domain.TicketStatusMachine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TicketDomainConfig {

    @Bean
    TicketStatusMachine ticketStatusMachine() {
        return new TicketStatusMachine();
    }
}
