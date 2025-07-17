package rinha_backend_2025.paymentgateway.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import rinha_backend_2025.paymentgateway.model.ProcessorHealth;
import rinha_backend_2025.paymentgateway.model.ProcessorType;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class HealthCheckService {

    private static final Duration TTL = Duration.ofSeconds(5); // cache por 5s
    private static final Duration TIMEOUT = Duration.ofMillis(300); // tempo máx por chamada

    private final WebClient.Builder webClientBuilder;
    private final Map<ProcessorType, CachedHealth> cache = new EnumMap<>(ProcessorType.class);
    private final Map<ProcessorType, String> processorUrls = new EnumMap<>(ProcessorType.class);

    public HealthCheckService(WebClient.Builder builder) {
        this.webClientBuilder = builder;

        //processorUrls.put(ProcessorType.DEFAULT, "http://localhost:8001");
        //processorUrls.put(ProcessorType.FALLBACK, "http://localhost:8002");

        processorUrls.put(ProcessorType.DEFAULT, System.getenv("PAYMENT_PROCESSOR_URL_DEFAULT"));
        processorUrls.put(ProcessorType.FALLBACK, System.getenv("PAYMENT_PROCESSOR_URL_FALLBACK"));
    }

    public Optional<ProcessorHealth> getHealth(ProcessorType type) {
        Instant now = Instant.now();
        CachedHealth cached = cache.get(type);

        if (cached != null && now.isBefore(cached.expiresAt())) {
            return Optional.of(cached.health());
        }

        try {
            String baseUrl = processorUrls.get(type);
            if (baseUrl == null) return Optional.empty();

            WebClient client = webClientBuilder.baseUrl(baseUrl).build();

            ProcessorHealth health = client.get()
                    .uri("/payments/service-health")
                    .retrieve()
                    .bodyToMono(ProcessorHealth.class)
                    .timeout(TIMEOUT)
                    .block();

            cache.put(type, new CachedHealth(health, now.plus(TTL)));
            return Optional.ofNullable(health);

        } catch (Exception e) {
            log.warn("Erro ao consultar health de {}: {}", type, e.getMessage());
            return Optional.empty();
        }
    }

    private record CachedHealth(ProcessorHealth health, Instant expiresAt) {}
}