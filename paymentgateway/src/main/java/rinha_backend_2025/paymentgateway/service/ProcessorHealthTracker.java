package rinha_backend_2025.paymentgateway.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import rinha_backend_2025.paymentgateway.model.ProcessorType;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;

@Slf4j
@Component
public class ProcessorHealthTracker {

    private static final int MAX_FAILURES = 3;
    private static final long OPEN_DURATION_MS = 5000;

    private final Map<ProcessorType, Integer> failureCounts = new EnumMap<>(ProcessorType.class);
    private final Map<ProcessorType, Instant> circuitOpenedAt = new EnumMap<>(ProcessorType.class);

    public synchronized void reportSuccess(ProcessorType type) {
        failureCounts.put(type, 0);
        circuitOpenedAt.remove(type);
    }

    public synchronized void reportFailure(ProcessorType type) {
        int failures = failureCounts.getOrDefault(type, 0) + 1;
        failureCounts.put(type, failures);

        if (failures >= MAX_FAILURES) {
            circuitOpenedAt.put(type, Instant.now());
            log.warn("Circuito de {} aberto por {} ms", type, OPEN_DURATION_MS);
        }
    }

    public synchronized boolean isCircuitOpen(ProcessorType type) {
        Instant opened = circuitOpenedAt.get(type);
        if (opened == null) return false;

        if (Instant.now().isAfter(opened.plusMillis(OPEN_DURATION_MS))) {
            log.info("Reabrindo circuito de {}", type);
            circuitOpenedAt.remove(type);
            failureCounts.put(type, 0);
            return false;
        }

        return true;
    }
}
