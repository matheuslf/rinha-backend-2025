FROM eclipse-temurin:21-jdk as builder
WORKDIR /app
COPY . .

RUN chmod +x mvnw

RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY --from=builder /app/target/payment-gateway-*.jar app.jar

EXPOSE 9999

ENTRYPOINT ["java", "-jar", "app.jar"]