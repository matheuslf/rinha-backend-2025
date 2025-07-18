package rinha_backend_2025.paymentgateway.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class PaymentResult {

    private final UUID correlationId;
    private final BigDecimal amount;
    private final ProcessorType processorType;
    //private final BigDecimal fee;
    private final boolean success;
    private final Instant processedAt;

    public PaymentResult(UUID correlationId,
                         BigDecimal amount,
                         ProcessorType processorType,
                         //                     BigDecimal fee,
                         boolean success) {
        this.correlationId = correlationId;
        this.amount = amount;
        this.processorType = processorType;
        //    this.fee = fee;
        this.success = success;
        this.processedAt = Instant.now();
    }

    public UUID getCorrelationId() {
        return correlationId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public ProcessorType getProcessorType() {
        return processorType;
    }

    //public BigDecimal getFee() {
    //    return fee;
    //}

    public boolean isSuccess() {
        return success;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}