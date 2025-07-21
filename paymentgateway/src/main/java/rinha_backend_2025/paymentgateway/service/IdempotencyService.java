package rinha_backend_2025.paymentgateway.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final StringRedisTemplate redisTemplate;

    // segurança para reprocessamento acidental
    private static final Duration TTL = Duration.ofMinutes(10);

    /**
     * Marca como processado se ainda não foi processado.
     * @return true se for novo, false se já foi processado.
     */
    public boolean markIfNew(UUID correlationId) {
        Boolean wasSet = redisTemplate.opsForValue()
                .setIfAbsent("processed:" + correlationId, "1", TTL);

        return Boolean.TRUE.equals(wasSet);
    }
}
