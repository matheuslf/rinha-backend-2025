# 🏆 Rinha de Backend 2025 - Java com Spring Boot

Submissão desenvolvida em Java 21 com Spring Boot + WebFlux para performance assíncrona.

🚀 Repositório: [[github.com/matheuslf/rinha-backend-2025](https://github.com/matheuslf/rinha-backend-2025/)]

## Estratégia

- WebClient com timeout customizado
- Fila interna concorrente (ConcurrentLinkedQueue)
- HealthCheck com cache de 5s e fallback automático
- Foco em: mínima taxa (preferência para DEFAULT), resiliência e throughput
- Exposto na porta 9999

### Fluxograma de Classes

<img width="1024" height="1536" alt="image" src="https://github.com/user-attachments/assets/ad2f5ff0-ff5a-47f2-a879-7459dcc3f8f2" />


Feito por: Matheus Leandro Ferreira | Lucas Eduardo | Luiz Castro
