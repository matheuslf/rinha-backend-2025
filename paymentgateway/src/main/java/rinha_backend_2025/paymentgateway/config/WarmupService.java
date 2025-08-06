package rinha_backend_2025.paymentgateway.config;

import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.concurrent.*;

@Component
public class WarmupService {

    public void run(WebClient webclient, String serverPort) {

        final String url = "http://localhost:" + serverPort + "/payments";
        final String payloadTemplate = """
                {"correlationId":"%s","amount":19.9}
                """;

        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        final int parallelism = getEnvOrDefault("REQUEST_WARMUP_PARALLELISM", 10);
        final int requests = getEnvOrDefault("REQUEST_WARMUP_COUNT", 100);

        System.out.println("[warmup] Iniciando warmup com " + requests + " requests paralelos...");

        ExecutorService executor = Executors.newFixedThreadPool(parallelism);

        try {
            List<CompletableFuture<Void>> futures = new ArrayList<>(requests);

            for (int i = 0; i < requests; i++) {
                String payload = String.format(payloadTemplate, UUID.randomUUID());

                futures.add(CompletableFuture.runAsync(() -> {
                    try {
                        webclient.post().uri(url).bodyValue(payload).retrieve();
                    } catch (Exception e) {
                        System.err.println("Erro ao enviar warmup: " + e.getMessage());
                    }
                }, executor));
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            System.out.println("[warmup] Finalizado warmup com sucesso ...");
        }
    }

    private int getEnvOrDefault(String key, int fallback) {
        try {
            String value = System.getenv(key);
            return (value != null && !value.isBlank()) ? Integer.parseInt(value) : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}