# Not alpine for the build stage: the gRPC code generator (protoc-gen-grpc-java, downloaded by the
# protobuf plugin) is linked against glibc and does not run on musl. The runtime stays on alpine.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY bookland-catalog-app/pom.xml bookland-catalog-app/
# Every module's pom, even those this image does not build: the parent lists them all, and the
# reactor refuses to start with one missing.
COPY bookland-app/pom.xml bookland-app/
COPY bookland-identity-app/pom.xml bookland-identity-app/
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

# Sources only of what this image builds: the catalog service is catalog + inventory.
COPY bookland-web-support/src bookland-web-support/src
COPY bookland-catalog/src bookland-catalog/src
COPY bookland-inventory/src bookland-inventory/src
COPY bookland-catalog-app/src bookland-catalog-app/src

RUN ./mvnw clean package -pl bookland-catalog-app -am -DskipTests -B

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/bookland-catalog-app/target/*.jar app.jar
# 8082 for HTTP; 9090 for gRPC, reached only inside the compose network.
EXPOSE 8082 9090
ENTRYPOINT ["java", "-jar", "app.jar"]
