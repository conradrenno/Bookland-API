# Not alpine for the build stage: the gRPC code generator (protoc-gen-grpc-java, downloaded by the
# protobuf plugin) is linked against glibc and does not run on musl ("program not found or is not
# executable"). The runtime stage below stays on alpine — nothing native runs there.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY bookland-app/pom.xml bookland-app/
# Every module's pom, even those this image does not build: the parent lists them all, and the
# reactor refuses to start with one missing.
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
COPY bookland-notification-app/pom.xml bookland-notification-app/

RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Sources only of what this image builds: user and auth run in the identity service's image, catalog
# and inventory in the catalog service's.
COPY bookland-web-support/src bookland-web-support/src
COPY bookland-orders/src bookland-orders/src
COPY bookland-reviews/src bookland-reviews/src
COPY bookland-wishlist/src bookland-wishlist/src
COPY bookland-payments/src bookland-payments/src
COPY bookland-app/src bookland-app/src

RUN ./mvnw clean package -pl bookland-app -am -DskipTests -B

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/bookland-app/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
