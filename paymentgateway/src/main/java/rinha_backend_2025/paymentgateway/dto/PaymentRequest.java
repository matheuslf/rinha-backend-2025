package rinha_backend_2025.paymentgateway.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentRequest(
        String correlationId,
        BigDecimal amount
) {
    public ProcessorPaymentRequest toProcessorPayload(Instant requestedAt) {
        return new ProcessorPaymentRequest(this.correlationId, this.amount, requestedAt);
    }
}