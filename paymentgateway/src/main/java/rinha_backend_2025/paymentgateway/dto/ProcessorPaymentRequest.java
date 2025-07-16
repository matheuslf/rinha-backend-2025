package rinha_backend_2025.paymentgateway.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProcessorPaymentRequest(
        UUID correlationId,
        BigDecimal amount,
        Instant requestedAt
) {}