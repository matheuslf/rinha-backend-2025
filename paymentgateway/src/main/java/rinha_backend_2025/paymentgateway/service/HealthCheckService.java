package rinha_backend_2025.paymentgateway.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
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
        processorUrls.put(ProcessorType.DEFAULT, System.getenv("PAYMENT_PROCESSOR_URL_DEFAULT"));
        processorUrls.put(ProcessorType.FALLBACK, System.getenv("PAYMENT_PROCESSOR_URL_FALLBACK"));
    }

    public Optional<ProcessorHealth> getHealth(ProcessorType type) {
        Instant now = Instant.now();
        CachedHealth cached = cache.get(type);
        if (cached != null && now.isBefore(cached.expiresAt())) {
            return Optional.of(cached.health());
        }

        synchronized (type) {
            cached = cache.get(type);
            if (cached != null && now.isBefore(cached.expiresAt())) {
                return Optional.of(cached.health());
            }

            try {
                String baseUrl = processorUrls.get(type);
                if (baseUrl == null) return Optional.empty();

                WebClient client = webClientBuilder.baseUrl(baseUrl).build();

                log.warn("Consultando saude do servidor {}", type);

                ProcessorHealth health = client.get()
                        .uri("/payments/service-health")
                        .exchangeToMono(response -> {
                            if (response.statusCode().is2xxSuccessful()) {
                                log.warn("Servidor OK: {} Status Code: {}", type, response.statusCode());
                                return response.bodyToMono(ProcessorHealth.class);
                            } else {
                                return response.bodyToMono(String.class)
                                        .doOnNext(body -> log.debug("Corpo do erro: {}", body))
                                        .then(Mono.empty());
                            }
                        })
                        .timeout(TIMEOUT)
                        .onErrorResume(e -> {
                            log.warn("Erro ao consultar saude do servidor {}: {}", type, e.getMessage());
                            return Mono.empty();
                        })
                        .block();

                if (health != null) {
                    cache.put(type, new CachedHealth(health, now.plus(TTL)));
                    return Optional.of(health);
                } else {
                    return Optional.empty();
                }

            } catch (Exception e) {
                return Optional.empty();
            }
        }
    }

    private record CachedHealth(ProcessorHealth health, Instant expiresAt) {}
}
