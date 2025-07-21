package rinha_backend_2025.paymentgateway.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import rinha_backend_2025.paymentgateway.dto.PaymentRequest;
import rinha_backend_2025.paymentgateway.model.PaymentResult;
import rinha_backend_2025.paymentgateway.model.ProcessorType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentProcessorClient processorClient;
    private final IdempotencyService idempotencyService;

    private final Queue<PaymentRequest> mainQueue = new ConcurrentLinkedQueue<>();
    private final Queue<PaymentRequest> priorityQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentMap<ProcessorType, Queue<PaymentResult>> results = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        for (ProcessorType type : ProcessorType.values()) {
            results.put(type, new ConcurrentLinkedQueue<>());
        }
    }

    public void enqueue(PaymentRequest request) {
        if (idempotencyService.markIfNew(request.correlationId())) {
            mainQueue.offer(request);
        } else {
            log.warn("Duplicate detected at enqueue: {}", request.correlationId());
        }
    }

    @Scheduled(fixedDelay = 10)
    public void flush() {
        List<PaymentRequest> batch = new ArrayList<>();

        while (!priorityQueue.isEmpty()) {
            PaymentRequest req = priorityQueue.poll();
            if (req != null) {
                batch.add(req);
            }
        }

        while (!mainQueue.isEmpty()) {
            PaymentRequest req = mainQueue.poll();
            if (req != null) {
                batch.add(req);
            }
        }

        for (PaymentRequest req : batch) {
            try {
                PaymentResult result = processorClient.sendToBestProcessor(req);
                if (result.isSuccess()) {
                    ProcessorType processor = result.getProcessorUsed();
                    if (processor != null) {
                        results.get(processor).add(result);
                    }
                } else if (result.isShouldRequeue()) {
                    log.warn("Reenqueue solicitado para pagamento {}", result.getCorrelationId());
                    priorityQueue.offer(req); // com prioridade

                } else {
                    log.error("Falha definitiva para pagamento: {}", result.getCorrelationId());
                }

            } catch (Exception e) {
                log.error("Erro inesperado ao processar {}: {}", req.correlationId(), e.getMessage(), e);
                priorityQueue.offer(req); // erro = tentativa de novo
            }
        }
    }

    public Map<String, Map<String, Object>> getSummary(ZonedDateTime from, ZonedDateTime to) {
        Map<String, Map<String, Object>> summary = new HashMap<>();

        for (ProcessorType type : ProcessorType.values()) {
            List<PaymentResult> snapshot;
            synchronized (results.get(type)) {
                snapshot = new ArrayList<>(results.get(type));
            }

            List<PaymentResult> filtered = snapshot.stream()
                    .filter(r -> {
                        Instant ts = r.getProcessedAt();
                        return (ts != null) &&
                                (from == null || !ts.isBefore(from.toInstant())) &&
                                (to == null || !ts.isAfter(to.toInstant()));
                    })
                    .toList();

            int totalRequests = filtered.size();
            BigDecimal totalAmount = filtered.stream()
                    .map(PaymentResult::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Map<String, Object> data = new HashMap<>();
            data.put("totalRequests", totalRequests);
            data.put("totalAmount", totalAmount.setScale(2, RoundingMode.HALF_UP));

            summary.put(type.name().toLowerCase(), data);
        }

        return summary;
    }
}
