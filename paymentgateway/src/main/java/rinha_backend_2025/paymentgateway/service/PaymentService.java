package rinha_backend_2025.paymentgateway.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import rinha_backend_2025.paymentgateway.dto.PaymentRequest;
import rinha_backend_2025.paymentgateway.model.PaymentResult;
import rinha_backend_2025.paymentgateway.model.ProcessorType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentProcessorClient processorClient;
    private final RedisTemplate<String, Object> redisTemplate; 

    private final Queue<PaymentRequest> queue = new ConcurrentLinkedQueue<>();

    public void enqueue(PaymentRequest request) {
        queue.offer(request);
    }

    @Scheduled(fixedDelay = 10)
    public void flush() {
        List<PaymentRequest> batch = new ArrayList<>();
        while (!queue.isEmpty()) {
            PaymentRequest req = queue.poll();
            if (req != null) batch.add(req);
        }

        for (PaymentRequest req : batch) {
            try {
                PaymentResult result = processorClient.sendToBestProcessor(req);
                if (result.success()) {
                    String key = "payments:" + result.processorType().name().toLowerCase();
                    redisTemplate.opsForList().rightPush(key, result);
                }
            } catch (Exception e) {
                log.error("Erro ao processar pagamento: {}", e.getMessage());
            }
        }
    }

    public Map<String, Map<String, Object>> getSummary(ZonedDateTime from, ZonedDateTime to) {
        Map<String, Map<String, Object>> summary = new HashMap<>();

        for (ProcessorType type : ProcessorType.values()) {
            String key = "payments:" + type.name().toLowerCase();
            List<Object> resultsAsObjects = redisTemplate.opsForList().range(key, 0, -1);
            if (resultsAsObjects == null) continue;

            List<PaymentResult> results = resultsAsObjects.stream()
                    .map(obj -> (PaymentResult) obj)
                    .toList();

            List<PaymentResult> filtered = results.stream()
                    .filter(r -> {
                        Instant ts = r.processedAt();
                        return (from == null || ts.isAfter(from.toInstant()))
                                && (to == null || ts.isBefore(to.toInstant()));
                    })
                    .toList();

            int totalRequests = filtered.size();
            BigDecimal totalAmount = filtered.stream()
                    .map(PaymentResult::amount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Map<String, Object> data = new HashMap<>();
            data.put("totalRequests", totalRequests);
            data.put("totalAmount", totalAmount);

            summary.put(type.name().toLowerCase(), data);
        }
        return summary;
    }
}