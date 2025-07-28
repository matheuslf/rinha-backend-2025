package rinha_backend_2025.paymentgateway.payment.controller;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rinha_backend_2025.paymentgateway.payment.dto.request.PaymentRequest;
import rinha_backend_2025.paymentgateway.payment.dto.response.PaymentSummary;
import rinha_backend_2025.paymentgateway.payment.repository.PaymentRepository;
import rinha_backend_2025.paymentgateway.payment.service.PaymentService;

import java.time.OffsetDateTime;

@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentRepository repository;

    @PostMapping("/payments")
    public ResponseEntity<Void> receive(@RequestBody PaymentRequest paymentRequest) {
        paymentRequest.requestedAt = OffsetDateTime.now();
        boolean enqueued = paymentService.enqueue(paymentRequest);

        if (!enqueued) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build(); // 429
        }

        return ResponseEntity.accepted().build(); // 202
    }

    @GetMapping("/payments-summary")
    public PaymentSummary getPaymentsSummary(@RequestParam(required = false) OffsetDateTime from,
                                             @RequestParam(required = false) OffsetDateTime to) {
        return repository.summarize(from, to);
    }
}
