package rinha_backend_2025.paymentgateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@ConfigurationProperties(prefix = "payment.processor")
public class ProcessorProperties {
    private String urlDefault;
    private String urlFallback;
}
