package rinha_backend_2025.paymentgateway.payment.dto.response;

public record ProcessorHealth(boolean failing, int minResponseTime) {}
