package rinha_backend_2025.paymentgateway.processor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import rinha_backend_2025.paymentgateway.shared.enums.ProcessorType;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;

@Slf4j
@Component
public class ProcessorHealthTracker {

    private static final int FAILURE_THRESHOLD          = 4;
    private static final Duration RECOVERY_TIMEOUT      = Duration.ofSeconds(3);

    /* ------------------------------------------------------------------ */
    private static class ProcessorStatus {
        int     failureCount        = 0;
        Instant lastFailure         = null;
        boolean temporarilyFailing  = false;
    }

    private final Map<ProcessorType, ProcessorStatus> health =
            new EnumMap<>(ProcessorType.class);

    public ProcessorHealthTracker() {
        for (ProcessorType t : ProcessorType.values()) health.put(t, new ProcessorStatus());
    }

    /* ---------------- API --------------------------------------------- */

    public synchronized void reportSuccess(ProcessorType t) {
        ProcessorStatus s = health.get(t);
        if (s.temporarilyFailing) {
            log.info("Processor {} reabilitado (1 sucesso)", t);
            s.temporarilyFailing = false;
        }
        s.failureCount = 0;
    }

    public synchronized void reportFailure(ProcessorType t) {
        ProcessorStatus s = health.get(t);
        s.failureCount++;
        s.lastFailure = Instant.now();
        if (!s.temporarilyFailing && s.failureCount >= FAILURE_THRESHOLD) {
            s.temporarilyFailing = true;
            log.warn("Processor {} marcado como falho ({} falhas)", t, s.failureCount);
        }
    }

    public synchronized boolean isFailing(ProcessorType t) {
        ProcessorStatus s = health.get(t);
        if (!s.temporarilyFailing) return false;

        if (Duration.between(s.lastFailure, Instant.now()).compareTo(RECOVERY_TIMEOUT) > 0) {
            log.info("Timeout de recuperação expirou para {} → reteste", t);
            s.temporarilyFailing = false;
            s.failureCount = 0;
            return false;
        }
        return true;
    }

    /* conveniência para fail‑fast */
    public synchronized boolean isFailingBoth() {
        return isFailing(ProcessorType.DEFAULT) && isFailing(ProcessorType.FALLBACK);
    }
}
