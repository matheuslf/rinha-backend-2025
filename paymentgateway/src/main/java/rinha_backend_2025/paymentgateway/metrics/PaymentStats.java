package rinha_backend_2025.paymentgateway.metrics;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class PaymentStats {

    private final AtomicInteger totalRequests = new AtomicInteger(0);
    private final AtomicReference<BigDecimal> totalAmount = new AtomicReference<>(BigDecimal.ZERO);

    public void register(BigDecimal amount) {
        totalRequests.incrementAndGet();
        totalAmount.updateAndGet(current -> current.add(amount));
    }

    public int getTotalRequests() {
        return totalRequests.get();
    }

    public BigDecimal getTotalAmount() {
        return totalAmount.get();
    }


}
