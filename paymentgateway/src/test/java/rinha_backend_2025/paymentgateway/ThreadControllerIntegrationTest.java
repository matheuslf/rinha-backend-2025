package rinha_backend_2025.paymentgateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.web.reactive.server.WebTestClient;
import rinha_backend_2025.paymentgateway.payment.controller.ThreadController;

import static org.junit.jupiter.api.Assertions.assertTrue;


@WebFluxTest(controllers = ThreadController.class)
class ThreadControllerIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldUseVirtualThreadToHandleRequest() {
        webTestClient.get()
                .uri("/thread/name")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(response -> {
                    System.out.println("Thread info: " + response);
                    assertTrue(response.contains("VirtualThread"), "Expected a VirtualThread but got: " + response);
                });
    }
}