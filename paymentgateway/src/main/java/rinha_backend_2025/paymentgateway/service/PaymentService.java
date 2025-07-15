package rinha_backend_2025.paymentgateway.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import rinha_backend_2025.paymentgateway.dto.PaymentRequest;
import rinha_backend_2025.paymentgateway.metrics.PaymentStats;
import rinha_backend_2025.paymentgateway.model.PaymentResult;
import rinha_backend_2025.paymentgateway.model.ProcessorType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentProcessorClient processorClient;

    private final Queue<PaymentRequest> queue = new ConcurrentLinkedQueue<>();
    private final Map<ProcessorType, List<PaymentResult>> results = new ConcurrentHashMap<>();
    private final Map<ProcessorType, PaymentStats> stats = new ConcurrentHashMap<>();

    /**
     * Faz a inicialização automatica dos processadores.
     */
    @PostConstruct
    public void init() {
        results.put(ProcessorType.DEFAULT, Collections.synchronizedList(new ArrayList<>()));
        results.put(ProcessorType.FALLBACK, Collections.synchronizedList(new ArrayList<>()));

        // adciona stats do payment
        // evita qualquer problemas com `null` quando acessar `results.get(type)` ou `stats.get(type)`
        stats.put(ProcessorType.DEFAULT, new PaymentStats());
        stats.put(ProcessorType.FALLBACK, new PaymentStats());
    }

    /**
     * PASSO 1 - Adiciona toda requisição de pagamento na fila
     * @param request
     *          Requisições de pagamento advindas do controller.
     */
    public void enqueue(PaymentRequest request) {
        queue.offer(request);
    }

    /**
     * PASSO 2 - Faz o flush da fila a cada 10ms.
     */
    @Scheduled(fixedDelay = 10)
    public void flush() {
        List<PaymentRequest> batch = new ArrayList<>();
        while (!queue.isEmpty()) {
            PaymentRequest req = queue.poll();
            if (req != null) batch.add(req);
        }

        // Percorre e envia para o melhor "processador".
        for (PaymentRequest req : batch) {
            try {
                PaymentResult result = processorClient.sendToBestProcessor(req);
                if (result.isSuccess()) {
                    results.get(result.getProcessorType()).add(result);
                }
            } catch (Exception e) {
                log.error("Erro ao processar pagamento: {}", e.getMessage());
            }
        }
    }

    public Map<String, Map<String, Object>> getSummary(ZonedDateTime from, ZonedDateTime to) {
        Map<String, Map<String, Object>> summary = new HashMap<>();

        for (ProcessorType type : ProcessorType.values()) {
            List<PaymentResult> filtered = results.get(type).stream()
                    .filter(r -> {
                        Instant ts = r.getProcessedAt();
                        return (from == null || ts.isAfter(from.toInstant()))
                                && (to == null || ts.isBefore(to.toInstant()));
                    })
                    .toList();

            int totalRequests = filtered.size();
            BigDecimal totalAmount = filtered.stream()
                    .map(PaymentResult::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Map<String, Object> data = new HashMap<>();
            data.put("totalRequests", totalRequests);
            data.put("totalAmount", totalAmount);

            summary.put(type.name().toLowerCase(), data);
        }

        return summary;
    }
}
