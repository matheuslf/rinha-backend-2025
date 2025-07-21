package rinha_backend_2025.paymentgateway.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import rinha_backend_2025.paymentgateway.dto.PaymentRequest;
import rinha_backend_2025.paymentgateway.model.PaymentResult;
import rinha_backend_2025.paymentgateway.model.ProcessorHealth;
import rinha_backend_2025.paymentgateway.model.ProcessorType;

import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentProcessorClient {

    private final WebClient.Builder webClientBuilder;
    private final HealthCheckService healthCheckService;
    private final ProcessorHealthTracker healthTracker;

    private static final Duration TIMEOUT = Duration.ofMillis(500);

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    public PaymentResult sendToBestProcessor(PaymentRequest request) {
        Instant processedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        List<ProcessorType> ordered = new ArrayList<>();

        // Otimismo: tenta sempre o DEFAULT primeiro, exceto se claramente falho
        if (!healthTracker.isFailing(ProcessorType.DEFAULT)) {
            ordered.add(ProcessorType.DEFAULT);
        }
        if (!healthTracker.isFailing(ProcessorType.FALLBACK)) {
            ordered.add(ProcessorType.FALLBACK);
        }

        if (ordered.isEmpty()) {
            log.warn("Ambos os processadores marcados como falhos. Forçando tentativa padrão.");
            ordered = List.of(ProcessorType.DEFAULT, ProcessorType.FALLBACK);
        }

        for (ProcessorType type : ordered) {
            if (healthTracker.isFailing(type)) {
                log.warn("PULANDO {} por estar marcado como falho", type);
                continue;
            }

            Optional<PaymentResult> result = tryWithRetry(type, request, processedAt);
            if (result.isPresent()) return result.get();
        }

        log.error("Falha total: nenhum processador disponível para {}", request.correlationId());
        return new PaymentResult(
                request.correlationId(),
                request.amount().setScale(2, RoundingMode.HALF_UP),
                null,
                false,
                true,
                processedAt
        );
    }


    private Optional<PaymentResult> tryWithRetry(ProcessorType type, PaymentRequest request, Instant processedAt) {

        String url = getEnvUrl(type);
        WebClient client = webClientBuilder.baseUrl(url).build();
        var payload = request.toProcessorPayload(processedAt);

        try {
            log.warn("Payload {} → {}", type, MAPPER.writeValueAsString(payload));
        } catch (Exception e) {
            log.warn("Erro ao serializar payload para {}: {}", type, e.getMessage());
        }

        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                int finalAttempt = attempt;

                log.warn("Tentativa [{}] → {} -> {}", finalAttempt, type, url);

                Boolean success = client.post()
                        .uri("/payments")
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body(BodyInserters.fromValue(payload))
                        .exchangeToMono(response -> {
                            if (response.statusCode().is2xxSuccessful()) {
                                log.info("Sucesso [{}] em {} para {}", finalAttempt, type, request.correlationId());
                                return Mono.just(true);
                            } else {
                                return response.bodyToMono(String.class)
                                        .map(body -> {
                                            if (response.statusCode().value() == 422 &&
                                                    body.contains("CorrelationId already exists")) {
                                                log.warn("CorrelationId duplicado [{}] em {}: assumindo como sucesso (idempotente)", finalAttempt, type);
                                                return true; // Trata como sucesso
                                            }
                                            log.warn("✗ Falha [{}] em {}: status={} body={}", finalAttempt, type, response.statusCode(), body);
                                            return false;
                                        });
                            }
                        })
                        .timeout(TIMEOUT)
                        .onErrorResume(e -> {
                            log.warn("Erro [{}] em {}: {}", finalAttempt, type, e.toString());
                            return Mono.just(false);
                        })
                        .block();

                if (Boolean.TRUE.equals(success)) {
                    healthTracker.reportSuccess(type);
                    return Optional.of(new PaymentResult(
                            request.correlationId(),
                            request.amount().setScale(2, RoundingMode.HALF_UP),
                            type,
                            true,
                            false,
                            processedAt
                    ));
                }

                healthTracker.reportFailure(type);
                Thread.sleep(50L * attempt); // backoff: 50, 100, 150ms

            } catch (Exception e) {
                log.warn("Exceção [{}] em {}: {}", attempt, type, e.getMessage());
                healthTracker.reportFailure(type);
            }
        }

        return Optional.empty();
    }

    private String getEnvUrl(ProcessorType type) {

        String url = switch (type) {
            case DEFAULT -> System.getenv("PAYMENT_PROCESSOR_URL_DEFAULT");
            case FALLBACK -> System.getenv("PAYMENT_PROCESSOR_URL_FALLBACK");
        };
        if (url == null || url.isBlank()) {
            log.error("Variável de ambiente não definida para {}", type);
            return switch (type) {
                case DEFAULT -> "http://payment-processor-default:8080";
                case FALLBACK -> "http://payment-processor-fallback:8080";
            };
        }
        return url;
    }
}
