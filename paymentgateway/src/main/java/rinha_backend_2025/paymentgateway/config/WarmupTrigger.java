package rinha_backend_2025.paymentgateway.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class WarmupTrigger {

    private final WarmupService warmupService;
    private final RestTemplate restTemplate;

    @Value("${server.port}")
    private String serverPort;

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        warmupService.run(restTemplate, serverPort);
    }
}
