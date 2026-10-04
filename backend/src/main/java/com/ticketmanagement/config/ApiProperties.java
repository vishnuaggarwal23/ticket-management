package com.ticketmanagement.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.api")
public record ApiProperties(int pageSizeDefault, int pageSizeMax) {
}
