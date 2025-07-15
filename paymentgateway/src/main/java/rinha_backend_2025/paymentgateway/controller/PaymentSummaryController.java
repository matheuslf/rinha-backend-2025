package rinha_backend_2025.paymentgateway.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rinha_backend_2025.paymentgateway.service.PaymentService;

import java.time.ZonedDateTime;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/payments-summary")
public class PaymentSummaryController {

    private final PaymentService paymentService;

    @GetMapping
    public ResponseEntity<Map<String, Map<String, Object>>> getSummary(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime to
    ) {
        return ResponseEntity.ok(paymentService.getSummary(from, to));
    }
}
