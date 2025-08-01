package rinha_backend_2025.paymentgateway.payment.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import rinha_backend_2025.paymentgateway.payment.dto.request.PaymentRequest;
import rinha_backend_2025.paymentgateway.payment.repository.PaymentRepository;

import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository repository;
    private final RestTemplate restTemplate;
    private final ExecutorService executorService;

    // Executor para retries com delay controlado
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Value("${payment-processor.default.url}")
    private String defaultUrlRaw;

    @Value("${payment-processor.fallback.url}")
    private String fallbackUrlRaw;

    private String defaultUrl;
    private String fallbackUrl;

    @PostConstruct
    private void init() {
        this.defaultUrl = defaultUrlRaw + "/payments";
        this.fallbackUrl = fallbackUrlRaw + "/payments";
        log.info("Default service URL: {}", defaultUrl);
        log.info("Fallback service URL: {}", fallbackUrl);
    }

    public boolean enqueue(PaymentRequest request) {
        try {
            executorService.execute(() -> attemptSend(request, 0));
            return true;
        } catch (RejectedExecutionException e) {
            return false;
        }
    }

    private void attemptSend(PaymentRequest request, int attempt) {
        if (attempt >= 3) {
            log.error("Falha no pagamento {}", request.correlationId);
            return;
        }

        try {
            request.setDefaultTrue();
            restTemplate.postForObject(defaultUrl, request, Object.class);
            persistAsync(request);
        } catch (Exception e1) {
            try {
                request.setDefaultFalse();
                restTemplate.postForObject(fallbackUrl, request, Object.class);
                persistAsync(request);
            } catch (Exception e2) {
                retryWithDelay(request, attempt + 1);
            }
        }
    }

    private void retryWithDelay(PaymentRequest request, int nextAttempt) {
        scheduler.schedule(() -> attemptSend(request, nextAttempt), 10, TimeUnit.MILLISECONDS);
    }

    private void persistAsync(PaymentRequest request) {
        executorService.execute(() -> repository.save(request));
    }
}
