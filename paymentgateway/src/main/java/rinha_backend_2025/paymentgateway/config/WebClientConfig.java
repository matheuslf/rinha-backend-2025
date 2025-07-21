package rinha_backend_2025.paymentgateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.tcp.TcpClient;

import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {
        // Jackson customizado
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new ParameterNamesModule())
                .registerModule(new Jdk8Module())
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Encoder + Decoder com Jackson
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(config -> {
                    config.defaultCodecs().jackson2JsonEncoder(
                            new Jackson2JsonEncoder(mapper, MediaType.APPLICATION_JSON));
                    config.defaultCodecs().jackson2JsonDecoder(
                            new Jackson2JsonDecoder(mapper, MediaType.APPLICATION_JSON));
                })
                .build();

        // Timeout baixo pro contexto da Rinha
        TcpClient tcpClient = TcpClient
                .create(ConnectionProvider.newConnection())
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 500)
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(500, TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(500, TimeUnit.MILLISECONDS)));

        return WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .exchangeStrategies(strategies)
                .clientConnector(new ReactorClientHttpConnector(HttpClient.from(tcpClient)));
    }
}
