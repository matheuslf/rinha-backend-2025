package rinha_backend_2025.paymentgateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        int connectionTimeout = getEnvOrDefault("REQUEST_CONNECTION_TIMEOUT", 500);
        int readTimeout = getEnvOrDefault("REQUEST_READ_TIMEOUT", 1000);

        return new RestTemplateBuilder()
                .requestFactorySettings(settings -> settings
                        .withConnectTimeout(Duration.ofMillis(connectionTimeout))
                        .withReadTimeout(Duration.ofMillis(readTimeout)))
                .build();
    }

    private int getEnvOrDefault(String key, int fallback) {
        try {
            String value = System.getenv(key);
            return (value != null && !value.isBlank()) ? Integer.parseInt(value) : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
