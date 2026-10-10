# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -e -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -e -DskipTests clean package

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app
# Run as non-root
RUN useradd -r -u 1001 loanmust && mkdir -p /app/storage-data && chown loanmust /app/storage-data
COPY --from=build /app/target/loanmust.jar app.jar
USER loanmust
EXPOSE 8080
# Production-oriented overrides for the single application.properties (dev defaults otherwise).
ENV JPA_DDL_AUTO=validate \
    JPA_OPEN_IN_VIEW=false \
    THYMELEAF_CACHE=true \
    PORT=8080 \
    STORAGE_LOCAL_DIR=/app/storage-data \
    DEMO_SEED_TRANSACTIONS=false
VOLUME ["/app/storage-data"]
ENTRYPOINT ["java", "-jar", "app.jar"]
