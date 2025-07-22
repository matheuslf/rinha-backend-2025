package rinha_backend_2025.paymentgateway;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import rinha_backend_2025.paymentgateway.payment.service.PaymentService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

class PaymentServiceTest {

    private PaymentService paymentService;

    @BeforeEach
    void setup() {
       /* PaymentProcessorClient fakeClient = mock(PaymentProcessorClient.class);
        when(fakeClient.sendToBestProcessor(any()))
                .thenAnswer(invocation -> {
                    PaymentRequest req = invocation.getArgument(0);
                    return new PaymentResult(
                            req.correlationId(),
                            req.amount(),
                            ProcessorType.DEFAULT,
                            true
                    );
                });
        paymentService = new PaymentService(fakeClient);
        paymentService.init();*/
    }

    @Test
    void testEnqueueAndFlushAndSummary() {
        /**var request = new PaymentRequest("test-uuid", BigDecimal.valueOf(50.0));

        paymentService.enqueue(request);
        paymentService.flush();

        Map<String, Map<String, Object>> summary = paymentService.getSummary(null, null);
        Map<String, Object> defaultSummary = summary.get("default");

        assertNotNull(defaultSummary);
        assertEquals(1, defaultSummary.get("totalRequests"));
        assertEquals(BigDecimal.valueOf(50.0), defaultSummary.get("totalAmount"));**/
    }
}

