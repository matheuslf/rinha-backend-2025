package rinha_backend_2025.paymentgateway.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import rinha_backend_2025.paymentgateway.model.ProcessorType;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;

@Slf4j
@Component
public class ProcessorHealthTracker {

    private static final int FAILURE_THRESHOLD = 3;
    private static final int RECOVERY_SUCCESS_THRESHOLD = 2;
    private static final Duration RECOVERY_TIMEOUT = Duration.ofSeconds(5);

    private static class ProcessorStatus {
        int failureCount = 0;
        int recoverySuccesses = 0;
        Instant lastFailure = null;
        Instant lastSuccess = Instant.now();
        boolean temporarilyMarkedAsFailing = false;
    }

    private final Map<ProcessorType, ProcessorStatus> healthMap = new EnumMap<>(ProcessorType.class);

    public ProcessorHealthTracker() {
        for (ProcessorType type : ProcessorType.values()) {
            healthMap.put(type, new ProcessorStatus());
        }
    }

    public synchronized void reportSuccess(ProcessorType type) {
        ProcessorStatus status = healthMap.get(type);

        if (status.temporarilyMarkedAsFailing) {
            status.recoverySuccesses++;
            if (status.recoverySuccesses >= RECOVERY_SUCCESS_THRESHOLD) {
                log.info("Processor {} reabilitado após {} sucessos consecutivos", type, RECOVERY_SUCCESS_THRESHOLD);
                status.temporarilyMarkedAsFailing = false;
                status.failureCount = 0;
                status.recoverySuccesses = 0;
            }
        } else {
            status.failureCount = 0;
        }

        status.lastSuccess = Instant.now();
    }

    public synchronized void reportFailure(ProcessorType type) {
        ProcessorStatus status = healthMap.get(type);
        status.failureCount++;
        status.recoverySuccesses = 0;
        status.lastFailure = Instant.now();

        if (status.failureCount >= FAILURE_THRESHOLD) {
            status.temporarilyMarkedAsFailing = true;
            log.warn("Processor {} marcado como falho após {} falhas consecutivas", type, status.failureCount);
        } else {
            log.debug("Processor {} reportou falha {}x", type, status.failureCount);
        }
    }

    public synchronized boolean isFailing(ProcessorType type) {
        ProcessorStatus status = healthMap.get(type);

        if (!status.temporarilyMarkedAsFailing) return false;

        // Verifica timeout para permitir reteste
        Instant now = Instant.now();
        if (status.lastFailure != null && Duration.between(status.lastFailure, now).compareTo(RECOVERY_TIMEOUT) > 0) {
            log.info("Processor {} em modo de reteste após {}s", type, RECOVERY_TIMEOUT.toSeconds());
            status.temporarilyMarkedAsFailing = false;
            status.failureCount = 0;
            status.recoverySuccesses = 0;
            return false;
        }

        return true;
    }
}
