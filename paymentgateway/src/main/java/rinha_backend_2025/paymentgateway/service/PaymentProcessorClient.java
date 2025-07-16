package rinha_backend_2025.paymentgateway.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import rinha_backend_2025.paymentgateway.dto.PaymentRequest;
import rinha_backend_2025.paymentgateway.model.PaymentResult;
import rinha_backend_2025.paymentgateway.model.ProcessorHealth;
import rinha_backend_2025.paymentgateway.model.ProcessorType;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentProcessorClient {

    private final WebClient.Builder webClientBuilder;
//    private final ConfigService configService;
    private final HealthCheckService healthCheckService;
    private final ProcessorHealthTracker healthTracker;

    private static final Duration TIMEOUT = Duration.ofMillis(500);

    public PaymentResult sendToBestProcessor(PaymentRequest request) {
        ProcessorType preferred = decideProcessor();
        ProcessorType alternate = (preferred == ProcessorType.DEFAULT) ? ProcessorType.FALLBACK : ProcessorType.DEFAULT;


        if (healthTracker.isCircuitOpen(preferred)) {
            return tryWithRetry(alternate, request)
                    .orElse(failResult(request));
        }

        return tryWithRetry(preferred, request)
                .orElseGet(() -> {
                    if (!healthTracker.isCircuitOpen(alternate)) {
                        return tryWithRetry(alternate, request)
                                .orElse(failResult(request));
                    } else {
                        return failResult(request);
                    }
                });
    }


    private ProcessorType decideProcessor() {
        Optional<ProcessorHealth> healthOpt = healthCheckService.getHealth(ProcessorType.DEFAULT);
        if (healthOpt.isPresent() && !healthOpt.get().failing()) {
            return ProcessorType.DEFAULT;
        }
        return ProcessorType.FALLBACK;
    }

    private Optional<PaymentResult> tryWithRetry(ProcessorType type, PaymentRequest request) {
        String url = (type == ProcessorType.DEFAULT)
                ? System.getenv("PAYMENT_PROCESSOR_DEFAULT_URL")
                : System.getenv("PAYMENT_PROCESSOR_FALLBACK_URL");
        //BigDecimal fee = configService.getFee(type);

        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                WebClient client = webClientBuilder.baseUrl(url).build();

                client.post()
                        .uri("/payments")
                        .bodyValue(request.toProcessorPayload(Instant.now()))
                        .retrieve()
                        .toBodilessEntity()
                        .timeout(TIMEOUT)
                        .block();

                healthTracker.reportSuccess(type);

                return Optional.of(new PaymentResult(
                        request.correlationId(),
                        request.amount(),
                        type,
                //        null,
                        true
                ));
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
        //        BigDecimal.ZERO,
                false
        );
    }
}
