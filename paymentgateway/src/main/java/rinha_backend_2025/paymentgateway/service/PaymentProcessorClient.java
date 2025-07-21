package rinha_backend_2025.paymentgateway.service;

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

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentProcessorClient {

    private final WebClient.Builder webClientBuilder;
    private final HealthCheckService healthCheckService;
    private final ProcessorHealthTracker healthTracker;

    private static final Duration TIMEOUT = Duration.ofMillis(500);

    public PaymentResult sendToBestProcessor(PaymentRequest request) {
        ProcessorType preferred = decideProcessor();
        ProcessorType alternate = (preferred == ProcessorType.DEFAULT) ? ProcessorType.FALLBACK : ProcessorType.DEFAULT;

        return tryWithRetry(preferred, request)
                .or(() -> tryWithRetry(alternate, request))
                .orElse(failResult(request));
    }

    private ProcessorType decideProcessor() {
        Optional<ProcessorHealth> healthOpt = healthCheckService.getHealth(ProcessorType.DEFAULT);
        if (healthOpt.isPresent() && !healthOpt.get().failing()) {
            return ProcessorType.DEFAULT;
        }
        return ProcessorType.FALLBACK;
    }

    private Optional<PaymentResult> tryWithRetry(ProcessorType type, PaymentRequest request) {
        String url = System.getenv(
                (type == ProcessorType.DEFAULT)
                        ? "PAYMENT_PROCESSOR_URL_DEFAULT"
                        : "PAYMENT_PROCESSOR_URL_FALLBACK"
        );
        Instant requestedAt = Instant.now();

        log.warn("Enviando pagamento para o servidor {}", type);
        for (int attempt = 1; attempt <= 3; attempt++) {

            log.warn("Tantativa {}", attempt);

            try {
                WebClient client = webClientBuilder.baseUrl(url).build();
                int finalAttempt = attempt;
                Boolean success = client.post()
                        .uri("/payments")
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body(BodyInserters.fromValue(request.toProcessorPayload(requestedAt)))
                        .exchangeToMono(response -> {
                            if (response.statusCode().is2xxSuccessful()) {
                                log.warn("Sucesso ao enviar pagamento {}", type);
                                return Mono.just(true);
                            } else {
                                log.warn("Tentativa {} falhou com status {} no processor {}", finalAttempt, response.statusCode(), type);
                                return Mono.just(false);
                            }
                        })
                        .timeout(TIMEOUT)
                        .onErrorResume(e -> {
                            log.warn("Erro ao enviar pagamento para {}: {}", type, e.getMessage());
                            return Mono.just(false);
                        })
                        .block();

                if (Boolean.TRUE.equals(success)) {
                    healthTracker.reportSuccess(type);
                    return Optional.of(new PaymentResult(
                            request.correlationId(),
                            request.amount(),
                            type,
                            true
                    ));
                } else {
                    healthTracker.reportFailure(type);
                }

            } catch (Exception e) {
                log.warn("Tentativa {} falhou com {}: {}", attempt, type, e.getMessage());
                healthTracker.reportFailure(type);
                try {
                    Thread.sleep(50L * attempt); // backoff: 50ms, 100ms, 150ms
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        return Optional.empty();
    }


    private PaymentResult failResult(PaymentRequest request) {
        return new PaymentResult(
                request.correlationId(),
                request.amount(),
                null,
                false
        );
    }
}
