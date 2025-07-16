package rinha_backend_2025.paymentgateway.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public record PaymentRequest(
        UUID correlationId,
        BigDecimal amount
) {
    public ProcessorPaymentRequest toProcessorPayload(Instant requestedAt) {
        return new ProcessorPaymentRequest(this.correlationId, this.amount.setScale(2, RoundingMode.HALF_UP), requestedAt.truncatedTo(ChronoUnit.MILLIS));
    }
}