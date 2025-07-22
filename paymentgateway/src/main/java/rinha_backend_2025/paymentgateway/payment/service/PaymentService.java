package rinha_backend_2025.paymentgateway.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import rinha_backend_2025.paymentgateway.payment.dto.request.PaymentRequest;
import rinha_backend_2025.paymentgateway.payment.dto.response.PaymentResult;
import rinha_backend_2025.paymentgateway.shared.enums.ProcessorType;
import rinha_backend_2025.paymentgateway.processor.PaymentProcessorClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.LongAdder;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final int QUEUE_LIMIT = 8_000;
    private static final int MAX_BATCH   = 250;

    private final PaymentProcessorClient processorClient;

    /* ---------- Idempotência em memória ---------- */
    private static final Duration IDEMP_TTL = Duration.ofMinutes(10);
    private static final int CLEAN_EVERY_N = 2_000;

    private final ConcurrentHashMap<UUID, Instant> seen = new ConcurrentHashMap<>(65_536);
    private final LongAdder enqueueCount = new LongAdder();


    /* ---------------- Fila principal ---------------- */
    private final ArrayBlockingQueue<PaymentRequest> mainQueue = new ArrayBlockingQueue<>(QUEUE_LIMIT  - 2_000);
    private final ArrayBlockingQueue<PaymentRequest> priorityQueue = new ArrayBlockingQueue<>(2_000);

    private static final class PaymentMetrics {
        final LongAdder totalRequests = new LongAdder();
        final DoubleAdder totalAmount   = new DoubleAdder();

        void add(BigDecimal amount) {
            totalRequests.increment();
            totalAmount.add(amount.doubleValue());
        }
    }

    private final EnumMap<ProcessorType, PaymentMetrics> stats = new EnumMap<>(ProcessorType.class);
    {
        for (ProcessorType t : ProcessorType.values()) stats.put(t, new PaymentMetrics());
    }

    public void enqueue(PaymentRequest req) {

        Instant now  = Instant.now();
        Instant prev = seen.putIfAbsent(req.correlationId(), now);

        // Já tínhamos o ID e o TTL não expirou → descarta
        if (prev != null && Duration.between(prev, now).compareTo(IDEMP_TTL) < 0) {
            log.warn("Duplicate detected: {}", req.correlationId());
            return;
        }

        /* ---- limpeza preguiçosa a cada N inserções ---- */
        enqueueCount.increment();
        if (enqueueCount.longValue() % CLEAN_EVERY_N == 0) {
            purgeOldKeys(now);
        }

        /* ---- enfileira ---- */
        if (!mainQueue.offer(req)) {
            log.error("Fila cheia! DESCARTANDO {}", req.correlationId());
            seen.remove(req.correlationId()); // libera p/ reenvio
        }
    }

    /** Remove correlationIds expirados */
    private void purgeOldKeys(Instant now) {
        seen.entrySet().removeIf(e ->
                Duration.between(e.getValue(), now).compareTo(IDEMP_TTL) > 0);
    }

    /** roda a cada 5ms */
    @Scheduled(fixedDelay = 5)
    public void flush() {

        List<PaymentRequest> batch = new ArrayList<>(MAX_BATCH);

        priorityQueue.drainTo(batch, MAX_BATCH);
        if (batch.size() < MAX_BATCH) {
            mainQueue.drainTo(batch, MAX_BATCH - batch.size());
        }

        if (batch.isEmpty()) return;

        for (PaymentRequest req : batch) {
            try {
                PaymentResult res = processorClient.sendToBestProcessor(req);

                if (res.isShouldRequeue()) {
                    priorityQueue.offer(req);         // tenta de novo
                    continue;
                }

                if (res.isSuccess()) {
                    ProcessorType p = Optional.ofNullable(res.getProcessorUsed())
                            .orElse(ProcessorType.DEFAULT);
                    stats.get(p).add(res.getAmount());
                } else {
                    log.warn("Falha definitiva {} {}", res.getProcessorUsed(), res.getCorrelationId());
                }

            } catch (Exception e) {
                log.error("Erro {}: {}", req.correlationId(), e.getMessage());
                priorityQueue.offer(req);
            }
        }
    }

    /* ---------------- Summary endpoint ---------------- */

    public Map<String, Map<String, Object>> getSummary(ZonedDateTime from, ZonedDateTime to) {
        Map<String, Map<String, Object>> out = new HashMap<>();

        stats.forEach((proc, m) -> {
            Map<String, Object> o = new HashMap<>(2);
            o.put("totalRequests", m.totalRequests.sum());
            o.put("totalAmount",
                    BigDecimal.valueOf(m.totalAmount.sum()).setScale(2, RoundingMode.HALF_UP));
            out.put(proc.name().toLowerCase(), o);
        });

        return out;
    }
}