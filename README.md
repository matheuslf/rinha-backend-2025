# 🏆 Rinha de Backend 2025 - Java com Spring Boot

Submissão desenvolvida em Java 21 com Spring Boot.

🚀 Repositório: [[[github.com/matheuslf/rinha-backend-2025](https://github.com/matheuslf/rinha-backend-2025/tree/master/paymentgateway)]

Feito por: Matheus Leandro Ferreira | Lucas Eduardo | Luiz Castro

## Fluxograma Principal

<img width="1024" height="1536" alt="image" src="https://github.com/user-attachments/assets/03157529-ca8a-49c9-bf1b-582fc23d17e4" />


## Estratégia

- WebClient com timeout customizado
- Fila interna concorrente (ConcurrentLinkedQueue)
- HealthCheck com cache de 5s e fallback automático
- Foco em: mínima taxa (preferência para DEFAULT), resiliência e throughput
- Exposto na porta 9999




