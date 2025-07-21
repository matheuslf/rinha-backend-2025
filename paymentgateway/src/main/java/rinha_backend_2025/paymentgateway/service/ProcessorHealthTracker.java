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

    private final Map<ProcessorType, Integer> failureCounts = new EnumMap<>(ProcessorType.class);

    public synchronized void reportSuccess(ProcessorType type) {
        failureCounts.put(type, 0);
    }

    public synchronized void reportFailure(ProcessorType type) {
        int failures = failureCounts.getOrDefault(type, 0) + 1;
        failureCounts.put(type, failures);
    }
}
