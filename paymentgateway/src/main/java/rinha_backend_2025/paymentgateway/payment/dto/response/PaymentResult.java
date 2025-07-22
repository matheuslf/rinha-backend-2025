package rinha_backend_2025.paymentgateway.payment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import rinha_backend_2025.paymentgateway.shared.enums.ProcessorType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@ToString
@AllArgsConstructor
public class PaymentResult {
    private UUID correlationId;
    private BigDecimal amount;
    private ProcessorType processorUsed;
    private boolean success;
    private boolean shouldRequeue;
    private Instant processedAt;

    // Construtor para sucesso/erro sem reenqueue e sem processedAt (será setado depois)
    public PaymentResult(UUID correlationId, BigDecimal amount, ProcessorType processorUsed, boolean success) {
        this(correlationId, amount, processorUsed, success, false, null);
    }

    // Construtor para erro com flag de reenqueue
    public PaymentResult(UUID correlationId, BigDecimal amount, ProcessorType processorUsed, boolean success, boolean shouldRequeue) {
        this(correlationId, amount, processorUsed, success, shouldRequeue, null);
    }
}
