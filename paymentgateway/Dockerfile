FROM eclipse-temurin:21-jdk as builder
WORKDIR /app
COPY . .

RUN chmod +x mvnw

RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY --from=builder /app/target/paymentgateway-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "-Dcom.sun.management.jmxremote", \
            "-Djava.rmi.server.hostname=127.0.0.1", \
            "-Dcom.sun.management.jmxremote.rmi.port=7091", \
            "-Dcom.sun.management.jmxremote.port=7091", \
            "-Dcom.sun.management.jmxremote.authenticate=false", \
            "-Dcom.sun.management.jmxremote.ssl=false", \
            "-XX:MaxRAMPercentage=80.0", "app.jar"]