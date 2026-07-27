FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY bookland-app/pom.xml bookland-app/
COPY bookland-web-support/pom.xml bookland-web-support/
COPY bookland-user/pom.xml bookland-user/
COPY bookland-auth/pom.xml bookland-auth/
COPY bookland-catalog/pom.xml bookland-catalog/
COPY bookland-orders/pom.xml bookland-orders/
COPY bookland-reviews/pom.xml bookland-reviews/
COPY bookland-inventory/pom.xml bookland-inventory/
COPY bookland-wishlist/pom.xml bookland-wishlist/
COPY bookland-payments/pom.xml bookland-payments/

RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

COPY bookland-web-support/src bookland-web-support/src
COPY bookland-user/src bookland-user/src
COPY bookland-auth/src bookland-auth/src
COPY bookland-catalog/src bookland-catalog/src
COPY bookland-orders/src bookland-orders/src
COPY bookland-reviews/src bookland-reviews/src
COPY bookland-inventory/src bookland-inventory/src
COPY bookland-wishlist/src bookland-wishlist/src
COPY bookland-payments/src bookland-payments/src
COPY bookland-app/src bookland-app/src

RUN ./mvnw clean package -pl bookland-app -am -DskipTests -B

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/bookland-app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
