package rinha_backend_2025.paymentgateway.payment.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rinha_backend_2025.paymentgateway.payment.dto.request.PaymentRequest;
import rinha_backend_2025.paymentgateway.payment.service.PaymentService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<Void> receive(@RequestBody PaymentRequest request) {
        paymentService.enqueue(request);
        return ResponseEntity.accepted().build();
    }
}
