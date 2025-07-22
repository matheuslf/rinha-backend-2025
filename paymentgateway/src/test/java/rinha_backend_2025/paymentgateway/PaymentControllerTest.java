package rinha_backend_2025.paymentgateway;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import rinha_backend_2025.paymentgateway.payment.controller.PaymentController;
import rinha_backend_2025.paymentgateway.payment.service.PaymentService;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @Test
    void testPostPaymentReturnsAccepted() throws Exception {
        String body = """
            {
              "correlationId": "11111111-1111-1111-1111-111111111111",
              "amount": 29.99
            }
            """;

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isAccepted());
    }

    @Test
    void testGetSummaryReturnsOk() throws Exception {
        Mockito.when(paymentService.getSummary(null, null))
                .thenReturn(Map.of(
                        "default", Map.of("totalRequests", 0, "totalAmount", 0),
                        "fallback", Map.of("totalRequests", 0, "totalAmount", 0)
                ));

        mockMvc.perform(get("/payments/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.default.totalRequests").value(0))
                .andExpect(jsonPath("$.fallback.totalAmount").value(0));
    }

    @Test
    void testGetSummaryWithValidResults() throws Exception {
        Mockito.when(paymentService.getSummary(null, null))
                .thenReturn(Map.of(
                        "default", Map.of("totalRequests", 2, "totalAmount", 59.98),
                        "fallback", Map.of("totalRequests", 1, "totalAmount", 10.00)
                ));

        mockMvc.perform(get("/payments/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.default.totalRequests").value(2))
                .andExpect(jsonPath("$.default.totalAmount").value(59.98))
                .andExpect(jsonPath("$.fallback.totalRequests").value(1))
                .andExpect(jsonPath("$.fallback.totalAmount").value(10.00));
    }

    @Test
    void testPostPaymentAndThenSummaryReturnsValidResults() throws Exception {
        String body = """
        {
          "correlationId": "22222222-2222-2222-2222-222222222222",
          "amount": 29.99
        }
        """;

        // Simula chamada POST que apenas aceita
        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isAccepted());

        // Agora simulamos o summary retornando 1 transação processada
        Mockito.when(paymentService.getSummary(null, null))
                .thenReturn(Map.of(
                        "default", Map.of("totalRequests", 1, "totalAmount", 29.99),
                        "fallback", Map.of("totalRequests", 0, "totalAmount", 0)
                ));

        mockMvc.perform(get("/payments/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.default.totalRequests").value(1))
                .andExpect(jsonPath("$.default.totalAmount").value(29.99))
                .andExpect(jsonPath("$.fallback.totalRequests").value(0))
                .andExpect(jsonPath("$.fallback.totalAmount").value(0));
    }

}