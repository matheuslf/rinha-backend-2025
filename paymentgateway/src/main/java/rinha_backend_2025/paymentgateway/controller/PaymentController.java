package rinha_backend_2025.paymentgateway.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rinha_backend_2025.paymentgateway.dto.PaymentRequest;
import rinha_backend_2025.paymentgateway.service.PaymentService;

import java.time.ZonedDateTime;
import java.util.Map;

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
