package rinha_backend_2025.paymentgateway.processor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import rinha_backend_2025.paymentgateway.payment.dto.request.PaymentRequest;
import rinha_backend_2025.paymentgateway.payment.dto.response.PaymentResult;
import rinha_backend_2025.paymentgateway.shared.enums.ProcessorType;

import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
public class PaymentProcessorClient {

    private static final Duration TIMEOUT     = Duration.ofMillis(300);
    private static final int MAX_RETRIES = 2;
    private static final long MAX_BACKOFF = 500;

    private final ProcessorHealthTracker healthTracker;
    private final Map<ProcessorType, WebClient> clients;

    public PaymentProcessorClient(WebClient.Builder builder,
                                  ProcessorHealthTracker tracker) {
        this.healthTracker = tracker;
        this.clients = Map.of(
                ProcessorType.DEFAULT ,  builder.baseUrl(resolveBaseUrl(ProcessorType.DEFAULT )).build(),
                ProcessorType.FALLBACK,  builder.baseUrl(resolveBaseUrl(ProcessorType.FALLBACK)).build()
        );
    }

    public PaymentResult sendToBestProcessor(PaymentRequest req) {
        Instant processedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);

        /* FAIL‑FAST se ambos em circuito aberto ----------------------- */
        if (healthTracker.isFailingBoth()) {
            return failFast(req, processedAt);
        }

        List<ProcessorType> ordered = new ArrayList<>(2);
        if (!healthTracker.isFailing(ProcessorType.DEFAULT))   ordered.add(ProcessorType.DEFAULT);
        if (!healthTracker.isFailing(ProcessorType.FALLBACK)) ordered.add(ProcessorType.FALLBACK);

        if (ordered.isEmpty()) ordered = List.of(ProcessorType.DEFAULT, ProcessorType.FALLBACK);

        for (ProcessorType t : ordered) {
            Optional<PaymentResult> r = tryWithRetry(t, req, processedAt);
            if (r.isPresent()) return r.get();
        }
        return failFast(req, processedAt);
    }

    private Optional<PaymentResult> tryWithRetry(ProcessorType t,
                                                 PaymentRequest req,
                                                 Instant processedAt) {

        WebClient client = clients.get(t);
        var payload = req.toProcessorPayload(processedAt);

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                boolean ok = Boolean.TRUE.equals(client.post()
                        .uri("/payments")
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body(BodyInserters.fromValue(payload))
                        .exchangeToMono(resp -> {
                            if (resp.statusCode().is2xxSuccessful()) return Mono.just(true);
                            return resp.bodyToMono(String.class).defaultIfEmpty("")
                                    .map(body -> resp.statusCode().value() == 422
                                            && body.contains("CorrelationId already exists"));
                        })
                        .timeout(TIMEOUT)
                        .onErrorResume(e -> Mono.just(false))
                        .block());

                if (ok) {
                    healthTracker.reportSuccess(t);
                    return Optional.of(successResult(req, processedAt, t));
                }

                healthTracker.reportFailure(t);
                long backoff = Math.min(MAX_BACKOFF,
                        (1L << (attempt - 1)) * 50 + ThreadLocalRandom.current().nextLong(30));
                Thread.sleep(backoff);

            } catch (Exception e) {
                healthTracker.reportFailure(t);
            }
        }
        return Optional.empty();
    }

    /* ---------------- Helpers ---------------------------------------- */
    private static PaymentResult successResult(PaymentRequest req, Instant ts, ProcessorType p) {
        return new PaymentResult(req.correlationId(),
                req.amount().setScale(2, RoundingMode.HALF_UP),
                p, true, false, ts);
    }

    private static PaymentResult failFast(PaymentRequest req, Instant ts) {
        return new PaymentResult(req.correlationId(),
                req.amount().setScale(2, RoundingMode.HALF_UP),
                null, false, true, ts);
    }

    private static String resolveBaseUrl(ProcessorType t) {
        String env = switch (t) {
            case DEFAULT  -> System.getenv("PAYMENT_PROCESSOR_URL_DEFAULT");
            case FALLBACK -> System.getenv("PAYMENT_PROCESSOR_URL_FALLBACK");
        };
        if (env == null || env.isBlank()) {
            return switch (t) {
                case DEFAULT  -> "http://payment-processor-default:8080";
                case FALLBACK -> "http://payment-processor-fallback:8080";
            };
        }
        return env;
    }
}
