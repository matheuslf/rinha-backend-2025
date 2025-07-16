package rinha_backend_2025.paymentgateway.model;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResult(
        String correlationId,
        BigDecimal amount,
        ProcessorType processorType,
        boolean success,
        Instant processedAt
) {
    public PaymentResult(String correlationId, BigDecimal amount, ProcessorType processorType, boolean success) {
        this(correlationId, amount, processorType, success, Instant.now());
    }
}