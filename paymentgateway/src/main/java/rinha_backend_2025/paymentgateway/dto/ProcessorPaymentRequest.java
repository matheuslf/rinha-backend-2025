package rinha_backend_2025.paymentgateway.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ProcessorPaymentRequest(
        String correlationId,
        BigDecimal amount,
        Instant requestedAt
) {}