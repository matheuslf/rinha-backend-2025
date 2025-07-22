package rinha_backend_2025.paymentgateway.payment.dto.response;

import rinha_backend_2025.paymentgateway.shared.enums.ProcessorType;

import java.math.BigDecimal;

public record PaymentSummary(
        ProcessorType processor,
        long totalRequests,
        BigDecimal totalAmount
) {}