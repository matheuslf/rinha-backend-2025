package rinha_backend_2025.paymentgateway.payment.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import rinha_backend_2025.paymentgateway.payment.dto.response.ProcessorHealth;
import rinha_backend_2025.paymentgateway.shared.enums.ProcessorType;
import rinha_backend_2025.paymentgateway.processor.ProcessorHealthTracker;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class HealthCheckService {

    private static final Duration TTL = Duration.ofSeconds(5);
    private static final Duration TIMEOUT = Duration.ofMillis(250);

    private final WebClient.Builder webClientBuilder;
    private final ProcessorHealthTracker healthTracker;

    private final Map<ProcessorType, CachedHealth> cache = new EnumMap<>(ProcessorType.class);
    private final Map<ProcessorType, String> processorUrls = new EnumMap<>(ProcessorType.class);

    public HealthCheckService(WebClient.Builder builder, ProcessorHealthTracker tracker) {
        this.webClientBuilder = builder;
        this.healthTracker = tracker;
    }

    public Optional<ProcessorHealth> getHealth(ProcessorType type) {
        if (healthTracker.isFailing(type)) {
            log.warn("Processador {} já marcado como falho → ignorando consulta", type);
            return Optional.empty();
        }

        Instant now = Instant.now();

        CachedHealth result = cache.compute(type, (t, old) -> {
            if (old != null && now.isBefore(old.expiresAt())) {
                log.debug("Cache HIT para {}", t);
                return old;
            }

            log.debug("Cache MISS para {}. Consultando /service-health", t);

            String url = resolveBaseUrl(t);
            WebClient client = webClientBuilder.baseUrl(url).build();

            try {
                ProcessorHealth health = client.get()
                        .uri("/payments/service-health")
                        .retrieve()
                        .bodyToMono(ProcessorHealth.class)
                        .timeout(TIMEOUT)
                        .onErrorResume(e -> {
                            log.warn("Erro no health check de {}: {}", t, e.toString());
                            return Mono.empty();
                        })
                        .blockOptional()
                        .orElse(null);

                if (health != null) {
                    return new CachedHealth(health, now.plus(TTL));
                }

            } catch (Exception e) {
                log.warn("Exceção no health check de {}: {}", t, e.getMessage());
            }

            return null; // não conseguiu obter health
        });

        return result != null ? Optional.of(result.health()) : Optional.empty();
    }


    private String resolveBaseUrl(ProcessorType type) {
        return processorUrls.computeIfAbsent(type, t -> {
            String env = switch (t) {
                case DEFAULT -> System.getenv("PAYMENT_PROCESSOR_URL_DEFAULT");
                case FALLBACK -> System.getenv("PAYMENT_PROCESSOR_URL_FALLBACK");
            };
            if (env == null || env.isBlank()) {
                log.error("Variável de ambiente não definida para {}", t);
                env = switch (t) {
                    case DEFAULT -> "http://payment-processor-default:8080";
                    case FALLBACK -> "http://payment-processor-fallback:8080";
                };
            }
            return env;
        });
    }

    private record CachedHealth(ProcessorHealth health, Instant expiresAt) {}
}
