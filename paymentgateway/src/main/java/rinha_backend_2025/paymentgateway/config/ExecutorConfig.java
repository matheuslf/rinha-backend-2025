package rinha_backend_2025.paymentgateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

@Configuration
public class ExecutorConfig {

    @Bean
    public ExecutorService executorService(@Value("${THREAD_POOL_SIZE:15}") int threadPoolSize,
                                           @Value("${THREAD_QUEUE_SIZE:100}") int queueSize) {

        return new ThreadPoolExecutor(
                threadPoolSize,
                threadPoolSize,
                60, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(queueSize)
        );


    }
}
