FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY bookland-notification-app/pom.xml bookland-notification-app/
# Every module's pom, even those this image does not build: the parent lists them all, and the
# reactor refuses to start with one missing.
COPY bookland-app/pom.xml bookland-app/
COPY bookland-identity-app/pom.xml bookland-identity-app/
COPY bookland-catalog-app/pom.xml bookland-catalog-app/
COPY bookland-gateway/pom.xml bookland-gateway/
COPY bookland-web-support/pom.xml bookland-web-support/
COPY bookland-user/pom.xml bookland-user/
COPY bookland-auth/pom.xml bookland-auth/
COPY bookland-catalog/pom.xml bookland-catalog/
COPY bookland-orders/pom.xml bookland-orders/
COPY bookland-reviews/pom.xml bookland-reviews/
COPY bookland-inventory/pom.xml bookland-inventory/
COPY bookland-wishlist/pom.xml bookland-wishlist/
COPY bookland-payments/pom.xml bookland-payments/
COPY bookland-notification/pom.xml bookland-notification/

RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Sources only of what this image builds: the notification service is the notification module alone.
COPY bookland-notification/src bookland-notification/src
COPY bookland-notification-app/src bookland-notification-app/src

RUN ./mvnw clean package -pl bookland-notification-app -am -DskipTests -B

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/bookland-notification-app/target/*.jar app.jar
# No port: the service has no HTTP API. It only connects out — Kafka, RabbitMQ, SMTP, PostgreSQL.
ENTRYPOINT ["java", "-jar", "app.jar"]
