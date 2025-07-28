package rinha_backend_2025.paymentgateway.payment.repository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;
import rinha_backend_2025.paymentgateway.payment.dto.request.PaymentRequest;
import rinha_backend_2025.paymentgateway.payment.dto.response.PaymentSummary;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class PaymentRepository {

    private final RedisTemplate<String, String> redis;
    private static final String KEY = "payments";

    @PostConstruct
    public void warmUpConnection() {
        try {
            redis.opsForValue().get("warmup");
            redis.opsForZSet().count("warmup", 0, 1);
        } catch (Exception ignored) {
        }
    }

    public void save(PaymentRequest request) {
        String entry = "%s:%s:%s".formatted(request.correlationId, request.amount, request.isDefault);
        double score = request.requestedAt.toEpochSecond();

        redis.opsForZSet().add(KEY, entry, score);
    }

    public PaymentSummary summarize(OffsetDateTime from, OffsetDateTime to) {
        from = Objects.requireNonNullElse(from, OffsetDateTime.MIN);
        to = Objects.requireNonNullElse(to, OffsetDateTime.now());

        Set<String> entries = redis.opsForZSet().rangeByScore(KEY, from.toEpochSecond(), to.toEpochSecond());
        return calculate(entries);
    }

    private PaymentSummary calculate(Set<String> entries) {
        if (entries == null || entries.isEmpty()) {
            return null;
        }

        long defaultCount = 0, fallbackCount = 0;
        BigDecimal defaultTotal = BigDecimal.ZERO, fallbackTotal = BigDecimal.ZERO;

        for (String entry : entries) {
            String[] parts = entry.split(":");
            if (parts.length < 3) continue;

            BigDecimal amount = new BigDecimal(parts[1]);
            boolean isDefault = Boolean.parseBoolean(parts[2]);

            if (isDefault) {
                defaultCount++;
                defaultTotal = defaultTotal.add(amount);
            } else {
                fallbackCount++;
                fallbackTotal = fallbackTotal.add(amount);
            }
        }

        return new PaymentSummary(
                new PaymentSummary.Summary(defaultCount, defaultTotal),
                new PaymentSummary.Summary(fallbackCount, fallbackTotal)
        );
    }
}
