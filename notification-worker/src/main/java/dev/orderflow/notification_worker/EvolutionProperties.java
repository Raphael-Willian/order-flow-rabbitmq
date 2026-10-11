package dev.orderflow.notification_worker;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "evolution")
public record EvolutionProperties(String baseUrl, String apiKey, String instance) {




}
