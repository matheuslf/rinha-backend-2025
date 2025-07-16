//package rinha_backend_2025.paymentgateway.service;
//
//import jakarta.annotation.PostConstruct;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Service;
//import org.springframework.web.reactive.function.client.WebClient;
//import rinha_backend_2025.paymentgateway.model.ProcessorType;
//
//import java.math.BigDecimal;
//import java.time.Duration;
//import java.util.EnumMap;
//import java.util.Map;
//
///**
// * NÃO USADO! PENSEI EM FAZER UM TRAMBIQUE COM ELE, MAS O ZAN DISSE QUE NÃO PODE FAZER ISSO KKK
// */
//@Slf4j
//@Service
//public class ConfigService {
//
//    private final WebClient.Builder webClientBuilder;
//
//    private final Map<ProcessorType, BigDecimal> feeCache = new EnumMap<>(ProcessorType.class);
//    private final Map<ProcessorType, String> urls = new EnumMap<>(ProcessorType.class);
//
//    public ConfigService(WebClient.Builder builder) {
//        this.webClientBuilder = builder;
//
//        urls.put(ProcessorType.DEFAULT, System.getenv("PAYMENT_PROCESSOR_URL_DEFAULT"));
//        urls.put(ProcessorType.FALLBACK, System.getenv("PAYMENT_PROCESSOR_URL_FALLBACK"));
//    }
//
//    @PostConstruct
//    @Scheduled(fixedDelay = 5000)
//    public void refresh() {
//        for (ProcessorType type : ProcessorType.values()) {
//            try {
//                String baseUrl = urls.get(type);
//                if (baseUrl == null) continue;
//
//                WebClient client = webClientBuilder.baseUrl(baseUrl).build();
//
//                var response = client.get()
//                        .uri("/admin/payments-summary")
//                        .header("X-Rinha-Token", "123")
//                        .retrieve()
//                        .bodyToMono(PaymentSummary.class)
//                        .timeout(Duration.ofMillis(500))
//                        .block();
//
//                if (response != null) {
//                    feeCache.put(type, response.feePerTransaction());
//                    log.debug("Fee de {} atualizada para {}", type, response.feePerTransaction());
//                }
//
//            } catch (Exception e) {
//                log.warn("Erro ao consultar fee de {}: {}", type, e.getMessage());
//            }
//        }
//    }
//
//    public BigDecimal getFee(ProcessorType type) {
//        return feeCache.getOrDefault(type, BigDecimal.valueOf(0.02)); // fallback seguro
//    }
//
//    public record PaymentSummary(BigDecimal feePerTransaction) {}
//}