FROM eclipse-temurin:21-jdk as builder
WORKDIR /app
COPY . .
RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jdk
WORKDIR /app

RUN apt-get update && apt-get install -y netcat-openbsd && rm -rf /var/lib/apt/lists/*

COPY --from=builder /app/target/app.jar .

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]